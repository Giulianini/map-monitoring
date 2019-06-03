package it.unibo.pcd1819.mapmonitoring.guardian

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Identify, Props, Timers}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor.{DashboardIdentity, GuardianIdentity, IdentifyGuardian, SensorValue}
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor

import scala.collection.immutable.SortedSet

object GuardianActor{
  def props(patchName: String) = Props(new GuardianActor(patchName))

  private[this] final case object Tick

  sealed trait GuardianInput
  final case object EndAlert extends GuardianInput
  final case class IdentifyGuardian(s: String) extends GuardianInput
  final case class GuardianIdentity(patch: String)
  final case object DashboardIdentity extends GuardianInput
  final case class SensorValue(v: Double) extends GuardianInput

  def main(args: Array[String]): Unit = {
    val port = if(args.isEmpty) "0" else args(0)
    val patch = if(args.isEmpty || args.length == 1) randomPatch else args(1)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("guardian"))

    val system = ActorSystem(clusterName, config)
    system actorOf(GuardianActor.props(patch), "guardian")
  }
}

class GuardianActor(private val patchName: String) extends Actor with ActorLogging with Timers{

  private val cluster = Cluster(context.system)
  private val guardianId = cluster.selfMember.address.toString
  private val actualPatch = toPatch(patchName).get

  private var consensusParticipants: Seq[ActorRef] = Seq()
  private var dashboardLookUpTable: Seq[ActorRef] = Seq()

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
  }

  private def clusterBehaviour: Receive = {
    case MemberUp(member) => manageNewMember(member)
    case MemberDowned(member) => manageDeadMember(member)
    case GuardianIdentity(patch) => definePartnership(patch)
    case DashboardIdentity => defineDashboard()
    case IdentifyGuardian("sensor") => sender() ! SensorAgent.GuardianIdentity(patchName)
    case IdentifyGuardian("guardian") => sender() ! GuardianActor.GuardianIdentity(patchName)
    case IdentifyGuardian("dashboard") => sender() ! DashboardActor.GuardianExistence(cluster.selfMember, guardianId, toPatch(patchName).get)
  }

  private def sensorListening: Receive = {
    case SensorValue(value) => log info s"Guardian received $value"
  }

  override def receive: Receive = clusterBehaviour orElse  {
    case CurrentClusterState(members, _, _, _, _) =>
      manageStartUpMembers(members)
      context become listening
    case _ =>
  }

  private def listening: Receive = clusterBehaviour orElse sensorListening orElse {
    case _ =>
  }

  private def manageStartUpMembers(members: SortedSet[Member]): Unit = {
    val filtered = members.filter(member => member.status == MemberStatus.Up)
      .filter(member => member.hasRole("guardian") || member.hasRole("dashboard"))
      .partition(member => member.hasRole("guardian") || member.hasRole("dashboard"))
    val guardians = filtered._1
    val dashboards = filtered._2
    guardians.foreach(g => context.system.actorSelection(s"${g.address}/user/**") ! Identify("guardian"))
    dashboards.foreach(d => context.system.actorSelection(s"${d.address}/user/**") ! Identify("guardian"))
  }


  private def definePartnership(patch: String): Unit = {
    if (this.patchName == patch) {
      consensusParticipants = consensusParticipants :+ sender()
      log debug s"${consensusParticipants}"
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

  private def manageDeadMember(member: Member): Unit = {
    log info "HEY WE NEED TO FIX THIS PROBLEM AMIRITE"
  }
}