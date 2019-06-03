package it.unibo.pcd1819.mapmonitoring.view.utilities

import eu.hansolo.enzo.led.Led
import eu.hansolo.enzo.ledbargraph.LedBargraphBuilder
import javafx.event.EventHandler
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, HBox}

import scala.collection.mutable


class LedId(val index: (Int, Int)) extends Led() {
  override protected def getUserAgentStylesheet: String = getClass.getResource("/sheets/led.css").toExternalForm
}
object PatchControlFactory {
  private val DEFAULT_N_OF_LEDS = 5
  def makeControlsBox(anchorPane: AnchorPane, patchMatrixDim: (Int, Int), onLedClick: EventHandler[MouseEvent]): mutable.Map[(Int, Int), HBox] = {
    //CREATION LOGIC
    var indexToHbox = new mutable.HashMap[(Int, Int), HBox]()
    val anchorSize = (anchorPane.getMinWidth, anchorPane.getMinHeight)
    val patchSize: (Double, Double) = (anchorSize._1 / patchMatrixDim._2, anchorSize._2 / patchMatrixDim._1)

    0 until patchMatrixDim._2 foreach (column => {
      0 until patchMatrixDim._1 foreach (row => { //(0, 0) (0, 1)...
        //Led
        val patchLed = new LedId((row, column))
        patchLed.setFrameVisible(false)
        patchLed.setId("hansolo-led")
        //Ledbar
        val guardianLedbarBuilder: LedBargraphBuilder[_] = LedBargraphBuilder.create()
        guardianLedbarBuilder.noOfLeds(DEFAULT_N_OF_LEDS)
        val guardianLedbar = guardianLedbarBuilder.build()
        //Handler
        patchLed.setOnMouseClicked(onLedClick)
        val patchHboxControl = new HBox(patchLed, guardianLedbar)
        anchorPane.getChildren.add(patchHboxControl)
        AnchorPane.setTopAnchor(patchHboxControl, patchSize._2 * row)
        AnchorPane.setLeftAnchor(patchHboxControl, patchSize._1 * column)
        indexToHbox += (row, column) -> patchHboxControl
      })
    })
    indexToHbox
  }
}
