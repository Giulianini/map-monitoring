package it.unibo.pcd1819.mapmonitoring.view.utilities

//import it.unibo.pcd1819.mapmonitoring.model.Vector2D
import it.unibo.pcd1819.mapmonitoring.view.shapes.ShapeId
import it.unibo.pcd1819.mapmonitoring.view.shapes.Shapes.{CubeId, CylinderId, SphereId}
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType
import javafx.animation.AnimationTimer
import javafx.geometry.Point3D
import javafx.scene.paint.PhongMaterial


case class ShapeBuilder(/*shapeType: ShapeType.Value, radius: Double, position: Vector2D, polygonNumber: Int, id: Int*/) {
//  private var shape: ShapeId = _
//
//  shapeType match {
//    case ShapeType.CUBE => this.shape = new CubeId(radius, id)
//      this
//    case ShapeType.CYLINDER => this.shape = new CylinderId(radius, polygonNumber, id)
//      this
//    case _ => this.shape = new SphereId(radius, polygonNumber, id)
//      this
//  }
//  this.position(position)
//
//  private def position(position: Vector2D): ShapeBuilder = {
////    this.shape.setTranslateX(position.x)
////    this.shape.setTranslateY(position.y)
//    this
//  }
//
//  def material(material: PhongMaterial): ShapeBuilder = {
//    this.shape.setMaterial(material)
//    this
//  }
//
//
//  def rotateAnimation(speed: Int): ShapeBuilder = {
//    val timer: AnimationTimer = _ => this.shape.setRotate(this.shape.getRotate + speed)
//    this.shape.setRotationAxis(new Point3D(0, -1, 0))
//    timer.start()
//    this
//  }
//
//  def build(): ShapeId = this.shape
}
