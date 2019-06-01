package it.unibo.pcd1819.mapmonitoring.view.utilities

import it.unibo.pcd1819.mapmonitoring.view.shapes.ShapeId
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType._
import javafx.scene.image.Image
import javafx.scene.paint.{Color, PhongMaterial}

object ParticleDrawingUtils {
  private val HUE = 0
  private val BRIGHTNESS = 0.90
  private val DIFFUSE_IMAGE = new Image("/images/paperMapping.jpg")

  /*def createParticleShapes(particle: Particle, shapeType: Value, environmentSize: Vector2D, polygons: Int, logicSize: Double, id: Int): ShapeId = {
    val particleRadius = particle.mass / 5
    val chargeColor = particle.charge
    val color = Color.hsb(ParticleDrawingUtils.HUE, chargeColor, ParticleDrawingUtils.BRIGHTNESS)
    val posX: Double = (particle.position.x / logicSize) * environmentSize.x * 0.5 + environmentSize.x * 0.5
    val posY: Double = (particle.position.y / logicSize) * environmentSize.y * 0.5 + environmentSize.y * 0.5
    val material = new PhongMaterial(color, ParticleDrawingUtils.DIFFUSE_IMAGE, null, null, null)

    ShapeBuilder(shapeType, particleRadius, Vector2D(posX, posY), polygons, id)
      .material(material)
      .rotateAnimation(2)
      .build()
  }*/
}
