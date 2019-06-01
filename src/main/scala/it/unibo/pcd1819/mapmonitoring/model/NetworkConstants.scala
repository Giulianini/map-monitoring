package it.unibo.pcd1819.mapmonitoring.model

object NetworkConstants {
  val sensorSeedPort = 5000
  val guardianSeedPort = 5001
  val dashboardSeedPort = 5002

  val systemAddress = "127.0.0.1"
  val transportProtocol = "akka.tcp"
  val clusterName = "MapMonitoringCluster"
  val sensorSeedNode = s"$transportProtocol://$clusterName@$systemAddress:$sensorSeedPort"
  val guardianSeedNode = s"$transportProtocol://$clusterName@$systemAddress:$guardianSeedPort"
  val dashboardSeedNode = s"$transportProtocol://$clusterName@$systemAddress:$dashboardSeedPort"
}
