package it.unibo.pcd1819.mapmonitoring.guardian

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._

object GuardianActor {
  def props = Props(classOf[GuardianActor])

  private[this] final case object Tick

  sealed trait GuardianInput
  final case object Opinion extends GuardianInput
  final case object EndAlert extends GuardianInput
  final case class SensorValue(v: Double) extends GuardianInput
  final case class GuardianValue(avg: Double) extends GuardianInput

  def main(args: Array[String]): Unit = {
    val port = if(args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("guardian"))

    val system = ActorSystem(clusterName, config)
    val guardian = system actorOf(GuardianActor.props)
  }
}

class GuardianActor extends Actor with ActorLogging with Timers{

  private val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    super.preStart()
  }

  override def receive: Receive = {
    case _ =>
  }
}