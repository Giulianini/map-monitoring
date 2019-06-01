package ass32gc.trulyyours.sensor

import akka.actor.{Actor, ActorLogging, ActorSystem, Address, Identify, Props, Timers}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import ass32gc.trulyyours.model.NetworkConstants._
import ass32gc.trulyyours.sensor.SensorAgent._
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration


object SensorAgent {
  def props = Props(classOf[SensorAgent])

  private final case object TickKey
  private final case object Tick
  private final case class Responsiveness(updateSpeed: FiniteDuration)

  def main(args: Array[String]): Unit = {
    val port = if(args.isEmpty) "0" else args(0)
    val config =
      ConfigFactory.parseString(s"""akka.remote.netty.tcp.port=$port""")
        .withFallback(ConfigFactory.load("sensor"))

    val system = ActorSystem(clusterName, config)

    val sensor = if (port == "0") system actorOf(SensorAgent.props, "sensor") else system actorOf(SensorAgent.props, "sensorSeedNode")
  }
}

class SensorAgent extends Actor with ActorLogging with Timers{

  import ass32gc.trulyyours.model.Environment._

  import scala.concurrent.duration._
  import scala.util.Random

  private val cluster = Cluster(context.system)
  private var guardians: List[Address] = List()
  private var dashboards: List[Address] = List()

  private val decisionMaker = Random
  private val nature = pickNature
  private var y: Double = _
  private var x: Double = _

  override def preStart(): Unit = {
    super.preStart()
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    x = decisionMaker.nextDouble() * width
    y = decisionMaker.nextDouble() * height
    log info s"${nature.updateSpeed.toMillis}"
    //self ! Tick
  }

  override def receive: Receive = {
    case MemberUp(member) => manageNewMembers(member)
    case MemberDowned(member) => manageDeadMembers(member)
    case Tick =>
      val value = pickDecision
      val currentPatch = toPatch(Coordinate(x, y))
      if (currentPatch.nonEmpty){
        guardians.map(address => context.actorSelection(address.toString)).foreach(actor => {
          actor ! Identify
        })
      }
      context become {
        if (remainSilent(value.toInt)) silentMoving else
          if(currentPatch.isEmpty) silentWandering else moving
      }
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
    case _ => log error "A sensor is not meant to be contacted"
  }

  def silent: Receive = {
    case MemberUp(member) => manageNewMembers(member)
    case MemberDowned(member) => manageDeadMembers(member)
    case Tick =>
      val value = pickDecision
      context become {
        if (becomeTalkative(value.toInt)) receive else silentMoving
      }
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
    case _ => log error "A sensor is not meant to be contacted"
  }

  def moving: Receive = {
    case MemberUp(member) => manageNewMembers(member)
    case MemberDowned(member) => manageDeadMembers(member)
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      //TODO: communicate to view name and coordinates
      context unbecome()
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
    case _ => log error "A sensor is not meant to be contacted"
  }

  def silentMoving: Receive = {
    case MemberUp(member) => manageNewMembers(member)
    case MemberDowned(member) => manageDeadMembers(member)
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      //TODO: communicate to view name and coordinates
      context become silent
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
    case _ => log error "A sensor is not meant to be contacted"
  }

  def silentWandering: Receive = {
    case MemberUp(member) => manageNewMembers(member)
    case MemberDowned(member) => manageDeadMembers(member)
    case Tick =>
      val value = pickDecision
      move(value.toInt)
      val outside = toPatch(Coordinate(x, y)).isEmpty
      context become {
        if (outside) silentWandering else
          if (remainSilent(value.toInt)) silentMoving else moving
      }
      timers startSingleTimer(TickKey, Tick, nature.updateSpeed)
    case _ => log error "A sensor is not meant to be contacted"
  }

  private def move(v: Int): Unit = {
    x += chooseNextHorizontalMovement(v.toInt)
    y += chooseNextVerticalMovement(v.toInt)
  }

  private def pickDecision: Double = {
    decisionMaker.nextDouble() * decisionMaker.nextInt(100)
  }

  private def remainSilent(v: Int): Boolean = {
    v == 0 || v == 25 || v == 50 || v == 75 || v == 100
  }

  private def becomeTalkative(v: Int): Boolean = {
    v == 0 || v == 25 || v == 50 || v == 75 || v == 100
  }

  private def chooseNextHorizontalMovement(value: Int): Double = value match {
    case v if v > 0 && v < 25 => -0.5
    case v if v > 25 && v < 50 => -1.0
    case v if v > 50 && v < 75 => 1.0
    case v if v > 75 && v < 100 => 0.5
    case _ => 0
  }

  private def chooseNextVerticalMovement(value: Int): Double = value match {
    case v if v > 0 && v < 25 => 0.5
    case v if v > 25 && v < 50 => 1.0
    case v if v > 50 && v < 75 => -1.0
    case v if v > 75 && v < 100 => -0.5
    case _ => 0
  }

  private def pickNature: Responsiveness = {
    val chosen = decisionMaker.nextDouble()
    Responsiveness(((chosen + 0.5) * (decisionMaker.nextInt(1000) + 500)).milliseconds)
  }

  private def manageNewMembers(member: Member): Unit = {
    if (member.roles.contains("guardian")) {
      guardians ::= member.address
    }
    if (member.roles.contains("dashboard")) {
      dashboards ::= member.address
    }
  }

  private def manageDeadMembers(member: Member): Unit = {
    if (member.roles.contains("guardian")) {
      guardians = guardians filterNot(address => address == member.address)
    }
    if (member.roles.contains("dashboard")) {
      dashboards = dashboards filterNot(address => address == member.address)
    }
  }

}