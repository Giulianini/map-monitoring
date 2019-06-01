package it.unibo.pcd1819.mapmonitoring.view.screens

import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Point3D
import javafx.scene.{Camera, Group}
import javafx.scene.input.{MouseEvent, ScrollEvent}
import javafx.scene.layout.Pane
import javafx.scene.transform.Rotate

class MouseGroupZoomAndRotation private(private val group: Group,
                                        private val pane: Pane,
                                        private val camera: Camera,
                                        private val rotationPivot: Point3D) {
  private var anchorX: Double = _
  private var anchorY: Double = _
  private var anchorAngleX: Double = 0
  private var anchorAngleY: Double = 0
  private val angleX = new SimpleDoubleProperty(0)
  private val angleY = new SimpleDoubleProperty(0)

  def initialize(): Unit = {
    val xRotate = new Rotate(0, Rotate.X_AXIS)
    val yRotate = new Rotate(0, Rotate.Y_AXIS)
    this.group.getTransforms.addAll(xRotate, yRotate)
    xRotate.setPivotX(this.rotationPivot.getX)
    xRotate.setPivotY(this.rotationPivot.getY)
    xRotate.setPivotZ(this.rotationPivot.getZ)
    yRotate.setPivotX(this.rotationPivot.getX)
    yRotate.setPivotY(this.rotationPivot.getY)
    yRotate.setPivotZ(this.rotationPivot.getZ)
    xRotate.angleProperty.bind(this.angleX)
    yRotate.angleProperty.bind(this.angleY)
    this.pane.setOnMousePressed((event: MouseEvent) => {
      this.anchorX = event.getX
      this.anchorY = event.getY
      this.anchorAngleX = this.angleX.get
      this.anchorAngleY = this.angleY.get
    })
    this.pane.setOnMouseDragged((event: MouseEvent) => {
      this.angleX.set(this.anchorAngleX - (this.anchorY - event.getSceneY))
      this.angleY.set(this.anchorAngleY + (this.anchorX - event.getSceneX))
    })
    this.pane.addEventHandler(ScrollEvent.SCROLL, (event: ScrollEvent) => {
      val delta = event.getDeltaY
      this.camera.translateZProperty.set(this.camera.getTranslateZ + delta)
    })
  }

}
object MouseGroupZoomAndRotation {
  def apply(group: Group, pane: Pane, camera: Camera, rotationPivot: Point3D): MouseGroupZoomAndRotation
  = new MouseGroupZoomAndRotation(group, pane, camera, rotationPivot)

}


