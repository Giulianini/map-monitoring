package it.unibo.pcd1819.mapmonitoring.model

import akka.cluster.Member

object Utils {
  def locateClusterMember(m: Member, location: String) : String = {
    println(s"${m.address}$location${m.address.port.get}")
    s"${m.address}$location${m.address.port.get}"
  }
}


