package ass32gc

import ass32gc.trulyyours.dashboard.DashboardActor
import ass32gc.trulyyours.guardian.GuardianActor
import ass32gc.trulyyours.sensor.SensorAgent
import ass32gc.trulyyours.model.NetworkConstants._

object Launcher extends App {
  SensorAgent.main(Seq(sensorSeedPort.toString).toArray)
  GuardianActor.main(Seq(guardianSeedPort.toString).toArray)
  DashboardActor.main(Seq(dashboardSeedPort.toString).toArray)

  SensorAgent.main(Seq(5500.toString).toArray)
  SensorAgent.main(Seq.empty.toArray)
  GuardianActor.main(Seq.empty.toArray)
  GuardianActor.main(Seq.empty.toArray)

}