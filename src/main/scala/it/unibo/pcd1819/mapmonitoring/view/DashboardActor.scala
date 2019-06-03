package it.unibo.pcd1819.mapmonitoring.view

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberDowned, MemberUp}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor._

import scala.collection.immutable.SortedSet

object DashboardActor {
  def props = Props(new DashboardActor())

  sealed trait ViewInput
  final case class IdentifyDashboard(sender: String) extends ViewInput
  final case class SensorPosition(sensorId: String, c: Coordinate) extends ViewInput
  final case class GuardianExistence(member: Member, guardianId: String, patch: Patch) extends ViewInput
  final case class PreAlert(guardianId: String) extends ViewInput
  final case class PreAlertEnd(guardianId: String) extends ViewInput
  final case class Alert(patch: Patch) extends ViewInput

  def main(args: Array[String]): Unit = {
    val port = if(args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("dashboard"))

    val system = ActorSystem(clusterName, config)
    system actorOf(DashboardActor.props, "dashboard")
  }
}


class DashboardActor extends Actor with ActorLogging with Timers{

  private val cluster = Cluster(context.system)
  private var guardians: Map[String, Patch] = Map()
  private var guardianMembers: Map[Member, String] = Map()

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
  }

  override def receive: Receive = {
    case CurrentClusterState(members, _, _, _, _) => manageStartUpMembers(members)
    case IdentifyDashboard("sensor") => sender() ! SensorAgent.DashboardIdentity
    case IdentifyDashboard("guardian") => sender() ! GuardianActor.DashboardIdentity
    case MemberUp(m) => manageNewMember(m)
    case MemberDowned(m) => manageDeadMember(m)
    case GuardianExistence(member, id, patch) => manageNewGuardian(member, id, patch)
    case SensorPosition(id, c) =>
      log debug s"view received $id, $c"
    //TODO: update sensor position along with the provided coordinates.
    // Sensors will only send information if they are inside the map.
    case PreAlert(guardianId) =>
      log debug s"view received $guardianId is in prealert"
      //TODO: update the specified guardian state to be in pre-alert (yellow)
    case PreAlertEnd(guardian) =>
      log debug s"view received $guardian is no more in prealert"
    //TODO: update the specified guardian state to be normal (green)
    case Alert(patch) =>
      log debug s"view received ${patch.name} is in ALERT"
      // TODO: the specified Patch is in alert, every guardian of such a patch
      // is in alert (red) and the whole patch needs to display the state of alert in some way.
      // An action from the Dashboard is needed to restore the normal state of the patch guardians,
      // which will all stop monitoring until such a message is received. There might be the need for some sort
      // of acknowledgement towards the Dashboard from all the patch Guardians to confirm that
      // the normal execution of the system has been restored
  }

  private def manageStartUpMembers(members: SortedSet[Member]): Unit = {
    val filtered = members.filter(member => member.status == MemberStatus.Up)
      .filter(member => member.hasRole("guardian"))
    filtered.foreach(g => context.system.actorSelection(s"${g.address}/user/**") ! GuardianActor.IdentifyGuardian("dashboard"))
  }

  private def manageNewGuardian(member: Member, id: String, patch: Patch): Unit = {
    guardians = guardians + (id -> patch)
    guardianMembers = guardianMembers + (member -> id)
  }

  private def manageNewMember(member: Member): Unit = member match{
    case m if m.roles.contains("guardian") =>
      context.system.actorSelection(s"${m.address}/user/**") ! GuardianActor.IdentifyGuardian("dashboard")
    case _ =>
  }

  private def manageDeadMember(m: Member): Unit = {
    val disappearedGuardian = guardianMembers(m)
    guardians = guardians - guardianMembers(m)
    guardianMembers = guardianMembers - m
    //handle the disappearance of the guardian using disappearedGuardian a.k.a. the id
  }
}