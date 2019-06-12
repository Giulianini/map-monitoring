package it.unibo.pcd1819.mapmonitoring

import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor

object EasyLauncher extends App {
  SensorAgent.main(Seq(firstSeedPort.toString).toArray)
  GuardianActor.main(Seq("0", "0", secondSeedPort.toString, "A").toArray)
  DashboardActor.main(Seq(thirdSeedPort.toString).toArray)

  GuardianActor.main(Seq("1", "0", "0", "A").toArray)
  GuardianActor.main(Seq("2", "0", "0", "A").toArray)
  GuardianActor.main(Seq("3", "0", "0", "A").toArray)

  GuardianActor.main(Seq("0", "0", "0", "B").toArray)
  GuardianActor.main(Seq("1", "0", "0", "B").toArray)
  GuardianActor.main(Seq("2", "0", "0", "B").toArray)

  GuardianActor.main(Seq("0", "0", "0", "C").toArray)
  GuardianActor.main(Seq("1", "0", "0", "C").toArray)
  GuardianActor.main(Seq("2", "0", "0", "C").toArray)

  GuardianActor.main(Seq("0", "0", "0", "D").toArray)
  GuardianActor.main(Seq("1", "0", "0", "D").toArray)
  GuardianActor.main(Seq("2", "0", "0", "D").toArray)

  GuardianActor.main(Seq("0", "0", "0", "E").toArray)
  GuardianActor.main(Seq("1", "0", "0", "E").toArray)
  GuardianActor.main(Seq("2", "0", "0", "E").toArray)

  GuardianActor.main(Seq("0", "0", "0", "F").toArray)
  GuardianActor.main(Seq("1", "0", "0", "F").toArray)
  GuardianActor.main(Seq("2", "0", "0", "F").toArray)

  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
  SensorAgent.main(Seq.empty.toArray)
}