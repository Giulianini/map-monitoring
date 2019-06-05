package it.unibo.pcd1819.mapmonitoring.sensor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent._
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor

import scala.collection.immutable.SortedSet
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.Random

object SensorAgent {
  def props = Props(classOf[SensorAgent])

  sealed trait SensorInput
  final case class GuardianIdentity(patch: String) extends SensorInput
  final case object DashboardIdentity extends SensorInput

  private final case object TickKey
  private final case object Tick
  private final case class Responsiveness(updateSpeed: FiniteDuration)

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("sensor"))

    val system = ActorSystem(clusterName, config)
    system actorOf(SensorAgent.props, "sensor")
  }
}

class SensorAgent extends Actor with ActorLogging with Timers {
  private val cluster = Cluster(context.system)

  private val decisionMaker = Random
  private val nature = pickNature
  private val sensorId = cluster.selfMember.address.toString

  private var guardianLookUpTable: Map[String, Seq[ActorRef]] = Map()
  private var dashboardLookUpTable: Seq[ActorRef] = Seq()

  private var y: Double = _
  private var x: Double = _

  override def preStart(): Unit = {
    super.preStart()
    x = decisionMaker.nextDouble() * width
    y = decisionMaker.nextDouble() * height
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    log info s"Sensor agent ${self.path} started with a ${nature.updateSpeed.toMillis} ms Tick"
  }


  override def receive: Receive = clusterBehaviour orElse {
    case CurrentClusterState(members, _, _, _, _) =>
      manageStartUpMembers(members)
      self ! Tick
      context become talk
    case _ => log info "How is this possible"
  }

  private def clusterBehaviour: Receive = {
    case MemberUp(member) => manageNewMember(member)
    case MemberDowned(member) => manageDeadMember(member)
    case GuardianIdentity(patch) => manageGuardianLookUpTable(patch)
    case DashboardIdentity => manageDashboardLookUpTable()
  }

  private def talk: Receive = clusterBehaviour orElse {
    case Tick =>
      val value = pickDecision
//      log info s"$value"
      val currentPatch = toPatch(Coordinate(x, y))
      if (currentPatch.nonEmpty) {
        if (guardianLookUpTable.contains(currentPatch.get.name)) {
          guardianLookUpTable(currentPatch.get.name).foreach(ref => {
            ref ! GuardianActor.SensorValue(sensorId, value)
            log debug s"SENDING $value to $ref"
          })
        }
      }
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
      context become {
        if (remainSilent(value.toInt)) {
          log debug "talk to silentMoving"
          silentMoving
        } else {
          if (currentPatch.isEmpty) {
            log debug "talk to silentWandering"
            silentWandering
          } else {
            log debug "talk to moving"
            moving
          }
        }
      }
    case _ => log error s"$x A sensor is not meant to be contacted"
  }

  private def silent: Receive = clusterBehaviour orElse {
    case Tick =>
      val value = pickDecision
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
      context become {
        if (becomeTalkative(value.toInt)) {
          log debug "silent to talk"
          talk
        } else {
          log debug "silent to silentMoving"
          silentMoving
        }
      }
    case _ => log error "A sensor is not meant to be contacted"
  }

  private def moving: Receive = clusterBehaviour orElse {
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      log info s"$x $y"
      log debug s"Sending ${Coordinate(x, y)} to view telling I am ${cluster.selfMember.address}"
      dashboardLookUpTable.foreach(
        ref => ref ! DashboardActor.SensorPosition(sensorId, Coordinate(x, y)))
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
      log debug "moving to talk"
      context become talk
    case _ => log error "A sensor is not meant to be contacted"
  }

  private def silentMoving: Receive = clusterBehaviour orElse {
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      log info s"$x $y"
      log debug s"Sending ${Coordinate(x, y)} to view telling I am ${cluster.selfMember.address}"
      dashboardLookUpTable.foreach(
        ref => ref ! DashboardActor.SensorPosition(sensorId, Coordinate(x, y)))
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
      log debug "silentMoving to silent"
      context become silent
    case _ => log error "A sensor is not meant to be contacted"
  }

  private def silentWandering: Receive = clusterBehaviour orElse {
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      val outside = toPatch(Coordinate(x, y)).isEmpty
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
      if (!outside) {
        context become {
          if (remainSilent(value.toInt)) {
            log debug "silentWandering to silentMoving"
            silentMoving
          } else {
            log debug "silentWandering to moving"
            moving
          }
        }
      }
    case _ => log error "A sensor is not meant to be contacted"
  }

  private def move(v: Int): Unit = {
    x += chooseNextHorizontalMovement(v.toInt)
    y += chooseNextVerticalMovement(v.toInt)
  }

  private def pickDecision: Double = {
    decisionMaker.nextDouble() * 100
  }

  private def remainSilent(v: Int): Boolean = {
    v == 0 || v == 25 || v == 50 || v == 75 || v == 100
  }

  private def becomeTalkative(v: Int): Boolean = {
    v == 0 || v == 25 || v == 50 || v == 75 || v == 100
  }

  private def chooseNextHorizontalMovement(value: Int): Double = value match {
    case v if v > 0 && v < 25 => decisionMaker.nextInt(7) - 3
    case v if v > 25 && v < 50 => decisionMaker.nextInt(7) - 3
    case v if v > 50 && v < 75 => decisionMaker.nextInt(7) - 3
    case v if v > 75 && v < 100 => decisionMaker.nextInt(7) - 3
    case _ => 0
  }

  private def chooseNextVerticalMovement(value: Int): Double = value match {
    case v if v > 0 && v < 25 => decisionMaker.nextInt(7) - 3
    case v if v > 25 && v < 50 => decisionMaker.nextInt(7) - 3
    case v if v > 50 && v < 75 => decisionMaker.nextInt(7) - 3
    case v if v > 75 && v < 100 => decisionMaker.nextInt(7) - 3
    case _ => 0
  }

  private def pickNature: Responsiveness = {
    Responsiveness(decisionMaker.nextInt(500).milliseconds)
  }

  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("guardian") =>
      context.system.actorSelection(s"${m.address}/user/**") ! GuardianActor.IdentifyGuardian("sensor")
    case m if member.roles.contains("dashboard") =>
      context.system.actorSelection(s"${m.address}/user/**") ! DashboardActor.IdentifyDashboard("sensor")
    case _ =>
  }

  private def manageStartUpMembers(members: SortedSet[Member]): Unit = {
    val filtered = members.filter(member => member.status == MemberStatus.Up)
      .filter(member => member.hasRole("guardian") || member.hasRole("dashboard"))
      .partition(member => member.hasRole("guardian") || member.hasRole("dashboard"))
    val guardians = filtered._1
    val dashboards = filtered._2
    guardians.foreach(g => context.system.actorSelection(s"${g.address}/user/**") ! GuardianActor.IdentifyGuardian("sensor"))
    dashboards.foreach(d => context.system.actorSelection(s"${d.address}/user/**") ! GuardianActor.IdentifyGuardian("sensor"))
  }

  private def manageGuardianLookUpTable(patch: String): Unit = {
    val updatedValues = if (guardianLookUpTable.contains(patch)) guardianLookUpTable(patch) :+ sender() else Seq(sender())
    guardianLookUpTable = guardianLookUpTable + (patch -> updatedValues)
    log debug s"$guardianLookUpTable"
  }

  private def manageDashboardLookUpTable(): Unit = {
    dashboardLookUpTable = dashboardLookUpTable :+ sender()
    log debug s"$dashboardLookUpTable"
  }

  private def manageDeadMember(member: Member): Unit = {
    log info "HEY WE NEED TO FIX THIS PROBLEM AMIRITE"
  }
}