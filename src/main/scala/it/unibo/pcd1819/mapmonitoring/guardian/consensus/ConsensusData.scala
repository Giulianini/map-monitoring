package it.unibo.pcd1819.mapmonitoring.guardian.consensus

import it.unibo.pcd1819.mapmonitoring.guardian.GuardianInfo

private[guardian] final case class ConsensusData private (var values: Seq[GuardianInfo], var sentIndices: Seq[Int],
                                                          var step: Int, var receivedAmount: Int) {
  def notSent: Seq[GuardianInfo] = this.values filter (gi => !this.sentIndices.contains(gi.index))
  def markAsSent(data: Seq[GuardianInfo]): ConsensusData = this.copy(sentIndices = sentIndices ++ (data map (_.index)))
  def receive(info: Seq[GuardianInfo]): ConsensusData = this.copy(values = this.values ++ info,
    receivedAmount = this.receivedAmount + 1)
  def incStep(): ConsensusData = this.copy(step = this.step + 1)
  def resetReceived(): ConsensusData = this.copy(receivedAmount = 0)
  def decide(actors: Int): Boolean = this.values.map(_.value).count(identity[Boolean]) > (actors.toDouble / 2d)
}

private[guardian] object ConsensusData {
  def apply(): ConsensusData = new ConsensusData(Seq(), Seq(), 0, 0)
}
