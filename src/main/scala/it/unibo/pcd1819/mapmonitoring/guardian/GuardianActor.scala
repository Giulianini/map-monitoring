package it.unibo.pcd1819.mapmonitoring.guardian

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Identify, Props, ReceiveTimeout, Stash, Timers}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor._
import it.unibo.pcd1819.mapmonitoring.guardian.consensus.ConsensusData
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

object GuardianActor {
  def props(patchName: String, id: Int, leaderId: Int) = Props(new GuardianActor(patchName, id, leaderId))


  sealed trait GuardianInput
  final case class IdentifyGuardian(s: String) extends GuardianInput
  final case class GuardianIdentity(patch: String) extends GuardianInput
  final case object DashboardIdentity extends GuardianInput
  final case class SensorValue(id: String, v: Double) extends GuardianInput
  private final case object CalculateAverage extends GuardianInput
  private final case object PreAlertTick extends GuardianInput
  private final case object AverageTimer extends GuardianInput
  private final case object Tick extends GuardianInput
  private final case object PreAlert

  // consensus
  private final case object PollAlert // from leader
  private final case class AlertState(state: Boolean) // to leader
  private final case object Alert
  private final case object NoAlert
  final case object EndAlert

  // leader election
  private final case class Vote(id: Int)

  private final case object LeaderIdentity

  // TODO update serialization-bindings in cluster.conf
//  private sealed trait ConsensusMessage
//  private final case object Vote extends ConsensusMessage
//  private final case class NewGuardianData(data: Seq[GuardianInfo])
//  private final case object GuardianReceiveTimeout
//  private final case object Step

  private final case object ConsensusTickKey
  private final case object ConsensusTimeout
  private final case object ElectionTickKey
  private final case object ElectionTimeout

  private val maxFaultyActors = 3

  def main(args: Array[String]): Unit = {
    val id = args(0).toInt
    val leaderId = args(1).toInt
    val port = if (args.length < 3) "0" else args(2)
    val patch = if (args.length < 4) randomPatch else args(3)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("guardian"))

    val system = ActorSystem(clusterName, config)
    system actorOf(GuardianActor.props(patch, id, leaderId), "guardian")
  }
}

class GuardianActor(private val patchName: String, private val id: Int, private var leaderId: Int) extends Actor with ActorLogging with Timers with Stash {

  private val cluster = Cluster(context.system)
  private val guardianId = cluster.selfMember.address.toString
  private val actualPatch = toPatch(patchName).get
  private val averagePeriod = 100.milliseconds
  private val tickPeriod = 500.milliseconds

  private var consensusParticipants: Seq[ActorRef] = Seq()
  private var dashboardLookUpTable: Seq[ActorRef] = Seq()
  private var sensorValue: Map[String, Double] = Map()

  private var leader: ActorRef = _

  private var canPreAlert = true
  private var consensusData: ConsensusData = ConsensusData(false)
  private var patchAverageState: Double = 0.0
  private var currentPreAlertDuration: FiniteDuration = 0.milliseconds

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
  }

  override def receive: Receive = clusterBehaviour orElse {
    case CurrentClusterState(members, _, _, _, _) =>
      manageStartUpMembers(members)
      context become listening
      timers startPeriodicTimer(AverageTimer, CalculateAverage, averagePeriod)
      timers startPeriodicTimer(PreAlertTick, Tick, tickPeriod)
    case _ =>
  }

  private def clusterBehaviour: Receive = {
    case MemberUp(member) => manageNewMember(member)
    case MemberDowned(member) => manageDeadMember(member)
    case GuardianIdentity(patch) => definePartnership(patch)
    case DashboardIdentity => defineDashboard()
    case IdentifyGuardian("sensor") => sender() ! SensorAgent.GuardianIdentity(patchName)
    case IdentifyGuardian("guardian") => sender() ! GuardianActor.GuardianIdentity(patchName)
    case IdentifyGuardian("dashboard") => sender() ! DashboardActor.GuardianExistence(cluster.selfMember, guardianId, toPatch(patchName).get)
    case LeaderIdentity =>
      log info "Acking someone as leader"
      this.leader = sender()
  }

  private def listening: Receive = clusterBehaviour orElse sensorAnalysis orElse {
    case PreAlert => // leader only
      log info "Received pre alert"
      val leaderVote = currentPreAlertDuration > dangerDurationThreshold
      consensusParticipants foreach (_ ! PollAlert)
      timers startSingleTimer (ConsensusTickKey, ConsensusTimeout, 500.milliseconds)
      context become alertDecision(Seq(leaderVote))
    case PollAlert =>
      val vote = currentPreAlertDuration > dangerDurationThreshold
      sender() ! AlertState(vote)
    case Alert =>
      context become alert
    case NoAlert =>
      this.canPreAlert = true
  }

  private def voting(votes: Seq[Int]): Receive = clusterStash orElse {
    case Vote(i) if i != this.id =>
      timers cancel ElectionTickKey
      timers startSingleTimer (ElectionTickKey, ElectionTimeout, 1.seconds)
      context become voting(votes :+ i)
    case ElectionTimeout =>
      log info "Electing new leader..."
      decideLeader(votes)
    case LeaderIdentity =>
      log info "Acking new leader."
      this.leader = sender()
      context become listening
  }

  private def decideLeader(votes: Seq[Int]): Unit = {
    val winner = votes.min
    if (winner == this.id) {
      log info s"I won the election. [${this.id}]"
      consensusParticipants foreach (_ ! LeaderIdentity)
    }
  }

  private def alertDecision(votes: Seq[Boolean]): Receive = clusterStash orElse {
    case AlertState(state) =>
      log info s"Received alert state $state"
      timers cancel ConsensusTickKey
      timers startSingleTimer (ConsensusTickKey, ConsensusTimeout, 2.seconds)
      context become alertDecision(votes :+ state)
    case ConsensusTimeout =>
      val shouldAlert = this.decideAlert(votes)
      if (shouldAlert) {
        log info "Sending Alert"
        dashboardLookUpTable foreach (_ ! DashboardActor.Alert(this.actualPatch))
        consensusParticipants foreach (_ ! Alert)
        context become alert
      } else {
        log info "Sending NoAlert"
        this.canPreAlert = true
        consensusParticipants foreach (_ ! NoAlert)
        context become listening
      }
  }

  private def alert: Receive = clusterStash orElse {
    case EndAlert =>
      log info "End alert"
      this.canPreAlert = true
      this.currentPreAlertDuration = 0.milliseconds
      context become listening
  }

  private def decideAlert(votes: Seq[Boolean]): Boolean = votes.count(_ == true) > (votes.length.toDouble / 2d)

  private def sensorAnalysis: Receive = {
    case SensorValue(id, value) => manageNewSensorData(id, value)
    case CalculateAverage =>
      calculateAverage()
    case Tick =>
      manageElapsingTime()
  }

  private def clusterStash: Receive = {
    case MemberUp(_) => stash()
    case MemberDowned(_) => stash()
    case GuardianIdentity(_) => stash()
    case DashboardIdentity => stash()
    case IdentifyGuardian("sensor") => stash()
    case IdentifyGuardian("guardian") => stash()
    case IdentifyGuardian("dashboard") => stash()
  }

  private def manageElapsingTime(): Unit = {
    if (patchAverageState > dangerThreshold) {
      dashboardLookUpTable.foreach(ref => ref ! DashboardActor.PreAlert(guardianId))
      currentPreAlertDuration += tickPeriod
    }
    else {
      dashboardLookUpTable.foreach(ref => ref ! DashboardActor.PreAlertEnd(guardianId))
      currentPreAlertDuration = 0.milliseconds
    }

    if (currentPreAlertDuration > dangerDurationThreshold && this.canPreAlert) {
      this.canPreAlert = false
      dashboardLookUpTable.foreach(ref => ref ! DashboardActor.PreAlert(guardianId))
      this.leader ! PreAlert
    }
  }

  private def manageStartUpMembers(members: SortedSet[Member]): Unit = {
    val filtered = members
      .filter(member => member != cluster.selfMember)
      .filter(member => member.hasRole("guardian") || member.hasRole("dashboard"))
      .partition(member => member.hasRole("guardian"))
    val guardians = filtered._1
    val dashboards = filtered._2
    guardians.foreach(g => context.system.actorSelection(s"${g.address}/user/**") ! GuardianActor.IdentifyGuardian("guardian"))
    dashboards.foreach(d => context.system.actorSelection(s"${d.address}/user/**") ! DashboardActor.IdentifyDashboard("guardian"))
  }


  private def definePartnership(patch: String): Unit = {
    if (this.patchName == patch) {
      consensusParticipants = consensusParticipants :+ sender()
      if (this.id == this.leaderId) {
        log info s"I am the leader. [${this.id}]"
        this.leader = self
        sender() ! LeaderIdentity
      }
//      log info s"$consensusParticipants"
    }
  }
  private def defineDashboard(): Unit = {
    sender() ! DashboardActor.GuardianExistence(cluster.selfMember, guardianId, actualPatch)
    dashboardLookUpTable = dashboardLookUpTable :+ sender()
  }


  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("guardian") =>
      context.system.actorSelection(s"${m.address}/user/**") ! GuardianActor.IdentifyGuardian("guardian")
    case m if member.roles.contains("dashboard") =>
      context.system.actorSelection(s"${m.address}/user/**") ! DashboardActor.IdentifyDashboard("guardian")
    case _ =>
  }

  private def calculateAverage(): Unit = {
    val values = sensorValue.values
    patchAverageState = values.foldLeft(0.0)((aggregator, value) => aggregator + value)/values.size
    sensorValue = sensorValue.empty
  }

  private def manageDeadMember(member: Member): Unit = {
    consensusParticipants foreach (_ ! Vote(this.id))
    context become voting(Seq(this.id))
  }

  private def manageNewSensorData(id: String, value: Double): Unit = {
    sensorValue = sensorValue + (id -> value)
  }
}