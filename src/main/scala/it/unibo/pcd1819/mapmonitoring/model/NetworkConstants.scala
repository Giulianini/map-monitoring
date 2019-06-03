package it.unibo.pcd1819.mapmonitoring.model

object NetworkConstants {
  val firstSeedPort = 5000
  val secondSeedPort = 5001
  val thirdSeedPort = 5002

  val systemAddress = "127.0.0.1"
  val transportProtocol = "akka.tcp"
  val clusterName = "MapMonitoringCluster"
}
