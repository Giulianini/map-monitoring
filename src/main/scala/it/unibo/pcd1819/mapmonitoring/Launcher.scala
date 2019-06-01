package ass32gc

import it.unibo.pcd1819.mapmonitoring.guardian.GuardianActor
import it.unibo.pcd1819.mapmonitoring.sensor.SensorAgent
import it.unibo.pcd1819.mapmonitoring.model.NetworkConstants._
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor

object Launcher extends App {
  SensorAgent.main(Seq(sensorSeedPort.toString).toArray)
  GuardianActor.main(Seq(guardianSeedPort.toString).toArray)
  DashboardActor.main(Seq(dashboardSeedPort.toString).toArray)

  SensorAgent.main(Seq(5500.toString).toArray)
  SensorAgent.main(Seq.empty.toArray)
  GuardianActor.main(Seq.empty.toArray)
  GuardianActor.main(Seq.empty.toArray)
}