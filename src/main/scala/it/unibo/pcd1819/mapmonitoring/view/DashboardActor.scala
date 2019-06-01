package it.unibo.pcd1819.mapmonitoring.view

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.model.Environment._
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor._

object DashboardActor {
  def props = Props(classOf[DashboardActor])

  sealed trait ViewInput
  final case class SensorPosition(id: String, c: Coordinate) extends ViewInput
  final case class PreAlert(guardian: String) extends ViewInput
  final case class PreAlertEnd(guardian: String) extends ViewInput
  final case class Alert(patch: Patch) extends ViewInput

  def main(args: Array[String]): Unit = {
    val port = if(args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("dashboard"))

    val system = ActorSystem(clusterName, config)

    val dashboard = system actorOf(DashboardActor.props)
  }
}


class DashboardActor extends Actor with ActorLogging with Timers{

  override def preStart(): Unit = {
    super.preStart()
  }

  override def receive: Receive = {
    case SensorPosition(id, c) =>
    //TODO: update sensor position along with the provided coordinates.
    // Sensors will only send information if they are inside the map.
    case PreAlert(guardian) =>
      //TODO: update the specified guardian state to be in pre-alert (yellow)
    case PreAlertEnd(guardian) =>
      //TODO: update the specified guardian state to be normal (green)
    case Alert(patch) =>
      //TODO: the specified Patch is in alert, every guardian of such a patch
      // is in alert (red) and the whole patch needs to display the state of alert in some way.
      // An action from the Dashboard is needed to restore the normal state of the patch guardians,
      // which will all stop monitoring until such a message is received. There might be the need for some sort
      // of acknowledgement towards the Dashboard from all the patch Guardians to confirm that
      // the normal execution of the system has been restored
  }
}