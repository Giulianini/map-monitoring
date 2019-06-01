package ass32gc.trulyyours.model

import scala.concurrent.duration._

object Environment {
  val width: Double = 800
  val height: Double = 600

  val horizontal: Int = 4
  val vertical: Int = 3
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

  final case class Coordinate(x: Double, y: Double)
  final case class Boundary(northwest: Coordinate, southeast: Coordinate) {
    def inside(c: Coordinate): Boolean =
      c.x >= northwest.x && c.x <= southeast.x && c.y >= northwest.y && c.y <= southeast.y
  }
  final case class Patch(box: Boundary, name: String)

  def toPatch(c: Coordinate): Option[Patch] = patches.find(p => p.box.inside(c))
}
