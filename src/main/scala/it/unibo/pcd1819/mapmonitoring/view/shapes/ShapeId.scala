package it.unibo.pcd1819.mapmonitoring.view.shapes

import javafx.scene.shape.{Box, Cylinder, Shape3D, Sphere}

trait ShapeId extends Shape3D {
  def id: Int
}

object Shapes {
  class SphereId(radius: Double, polygons: Int, override val id: Int) extends Sphere(radius, polygons) with ShapeId {}
  class CubeId(side: Double, override val id: Int) extends Box(side, side, side) with ShapeId
  class CylinderId(radius: Double, polygons: Int, override val id: Int) extends Cylinder(radius, radius, polygons) with ShapeId
}
