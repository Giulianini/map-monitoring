package it.unibo.pcd1819.mapmonitoring.model

import scala.concurrent.duration._
import scala.util.Random

object Environment {
  final case class Coordinate(x: Double, y: Double)
  final case class Boundary(northwest: Coordinate, southeast: Coordinate) {
    def inside(c: Coordinate): Boolean =
      c.x >= northwest.x && c.x <= southeast.x && c.y >= northwest.y && c.y <= southeast.y
  }
  final case class Patch(box: Boundary, name: String)

  private val r = Random

  val width: Double = 600
  val height: Double = 400

  val horizontal: Int = 3
  val vertical: Int = 2
  val patchNumber: Int = horizontal * vertical

  val dangerThreshold: Double = 70.0
  val dangerDurationThreshold: FiniteDuration = 2000.milliseconds

  val patches: Seq[Patch] =
    (for (i <- 0 until vertical; j <- 0 until horizontal) yield (i, j))
    .map(pair => (Coordinate((width/horizontal)*pair._2, (height/vertical)*pair._1), Coordinate((width/horizontal)*(pair._2 + 1), (height/vertical)*(pair._1 + 1))))
    .map(pair => Boundary(pair._1, pair._2))
    .zip ('A' to 'Z')
    .map (pair => Patch(pair._1, pair._2.toString))

  val patchesName: Seq[String] = patches.map(patch => patch.name)

  def toPatch(name: String): Option[Patch] = patches.find(p => p.name == name)
  def toPatch(c: Coordinate): Option[Patch] = patches.find(p => p.box.inside(c))

  def randomPatch: String = patchesName(r.nextInt(patchesName.size))
}
