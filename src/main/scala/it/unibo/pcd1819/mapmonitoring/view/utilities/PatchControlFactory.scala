package it.unibo.pcd1819.mapmonitoring.view.utilities

import eu.hansolo.enzo.canvasled.Led
import it.unibo.pcd1819.mapmonitoring.model.Environment
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.control.Separator
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, HBox}

import scala.collection.mutable


sealed case class LedPatch(name: String) extends Led() {
  override protected def getUserAgentStylesheet: String = getClass.getResource("/sheets/led.css").toExternalForm
  this.setId("patch-led")
  this.setFrameVisible(false)
}
sealed case class LedGuardian(name: String) extends Led() {
  override protected def getUserAgentStylesheet: String = getClass.getResource("/sheets/led.css").toExternalForm
  this.setId("guardian-led")
  this.setFrameVisible(false)
}

object PatchControlFactory {
  def makeControlsBox(anchorPane: AnchorPane, onLedClick: EventHandler[MouseEvent]): mutable.Map[String, (LedPatch, HBox)] = {
    //CREATION LOGIC
    var indexToHbox = new mutable.HashMap[String, (LedPatch, HBox)]()

    Environment.patches.foreach(patch => {
      val guardianLedBox = new HBox()
      val patchLed = LedPatch(patch.name)
      patchLed.setOnMouseClicked(onLedClick)
      val patchHboxControl = new HBox(patchLed, guardianLedBox)
      anchorPane.getChildren.add(patchHboxControl)
      AnchorPane.setTopAnchor(patchHboxControl, patch.box.northwest.y)
      AnchorPane.setLeftAnchor(patchHboxControl, patch.box.northwest.x)
      indexToHbox += (patch.name) -> (patchLed, guardianLedBox)
    })
    indexToHbox
  }

  def makeSeparator(anchorPane: AnchorPane): Unit = {
    0 to Environment.column foreach (p => {
      val separator = new Separator(Orientation.VERTICAL)
      AnchorPane.setBottomAnchor(separator, 0.0)
      AnchorPane.setTopAnchor(separator, 0.0)
      AnchorPane.setLeftAnchor(separator, p.toDouble * (Environment.width / Environment.column).toInt)
      anchorPane.getChildren.add(separator)
    })
    0 to Environment.row foreach (p => {
      val separator = new Separator(Orientation.HORIZONTAL)
      AnchorPane.setLeftAnchor(separator, 0.0)
      AnchorPane.setRightAnchor(separator, 0.0)
      AnchorPane.setTopAnchor(separator, p.toDouble * (Environment.height / Environment.row).toInt)
      anchorPane.getChildren.add(separator)
    })
  }
}

