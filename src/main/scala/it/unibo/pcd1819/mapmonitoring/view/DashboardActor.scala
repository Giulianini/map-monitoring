package it.unibo.pcd1819.mapmonitoring.view

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor._
import it.unibo.pcd1819.mapmonitoring.view.screens.{ActorObserver, MainScreenView}

import scala.collection.immutable.SortedSet

case class DashboardActorState(guardians: Map[(ActorRef, Member, String), Patch])

class DashboardActor extends Actor with ActorLogging with Timers {
  private val cluster = Cluster(context.system)
  private val view: ActorObserver = MainScreenView()
  view.setViewActorRef(self)

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
  }
  override def receive: Receive = onMessage(DashboardActorState(Map()))

  private def onMessage(state: DashboardActorState): Receive = {
    case CurrentClusterState(members, _, _, _, _) => manageStartUpMembers(members)
    case IdentifyDashboard("sensor") => sender() ! SensorAgent.DashboardIdentity
    case IdentifyDashboard("guardian") => sender() ! GuardianActor.DashboardIdentity
    case MemberUp(m) => manageNewMember(m)
    case MemberDowned(m) => manageDeadMember(state, m)
    case GuardianExistence(member, id, patch) => manageNewGuardian(state, member, id, patch)
    case SensorPosition(id, c) => view.sensorPosition(id, c)
    case PreAlert(guardianId) => view.preAlert(guardianId)
    case PreAlertEnd(guardian) => view.preAlertEnd(guardian)
    case Alert(patch) => view.alert(patch)
    //FROM GUI
    case Log(message) => log info message
    case EndAlert(patch) => log info s"Ended alarm in patch: ${patch.name}"; endAlertAllGuardian(state: DashboardActorState, patch: Patch)
  }

  private def manageStartUpMembers(members: SortedSet[Member]): Unit = {
    val filtered = members.filter(member => member.status == MemberStatus.Up).filter(member => member.hasRole("guardian"))
    filtered.foreach(g => context.system.actorSelection(s"${g.address}/user/**") ! GuardianActor.IdentifyGuardian("dashboard"))
  }

  private def manageNewMember(member: Member): Unit = member match {
    case m if m.roles.contains("guardian") => context.system.actorSelection(s"${m.address}/user/**") ! GuardianActor.IdentifyGuardian("dashboard")
    case _ =>
  }

  private def manageNewGuardian(state: DashboardActorState, member: Member, id: String, patch: Patch): Unit = {
    context.become(onMessage(state.copy(state.guardians + ((sender(), member, id) -> patch))))
    view.guardianExistence(id, patch)
  }

  private def manageDeadMember(state: DashboardActorState, m: Member): Unit = {
    val key = state.guardians.find(p => p._1._2 == m).get._1
    context.become(onMessage(state.copy(state.guardians - key)))
  }

  private def endAlertAllGuardian(state: DashboardActorState, patch: Patch): Unit = {
    state.guardians.filter(pair => pair._2 == patch).map(p => p._1._1).foreach(g => {
      log info s"Guardian end alert: $g"
      g ! GuardianActor.EndAlert
    })
  }
}

object DashboardActor {
  def props = Props(new DashboardActor())

  def main(args: Array[String]): Unit = {
    val port = if (args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""").withFallback(ConfigFactory.load("dashboard"))
    val system = ActorSystem(clusterName, config)
    system actorOf(DashboardActor.props, "dashboard")
  }

  sealed trait ViewInput
  //FROM OUTSIDE
  final case class IdentifyDashboard(sender: String) extends ViewInput
  final case class SensorPosition(sensorId: String, c: Coordinate) extends ViewInput
  final case class GuardianExistence(member: Member, guardianId: String, patch: Patch) extends ViewInput
  final case class PreAlert(guardianId: String) extends ViewInput
  final case class PreAlertEnd(guardianId: String) extends ViewInput
  final case class Alert(patch: Patch) extends ViewInput
  //FROM GUI
  final case class EndAlert(patch: Patch) extends ViewInput
  final case class Log(message: String) extends ViewInput
}