package it.unibo.pcd1819.mapmonitoring.view.screens

import com.jfoenix.controls._
import eu.hansolo.enzo.ledbargraph.LedBargraphBuilder
import it.unibo.pcd1819.mapmonitoring.view.utilities.{JavafxEnums, LedId, PatchControlFactory, ViewUtilities}
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.{AnchorPane, BorderPane, HBox}
import org.kordamp.ikonli.material.Material

import scala.collection.mutable

trait View {
  def startSimulation(): Unit
  def stopSimulation(): Unit
}

protected abstract class AbstractMainScreenView() extends View {
  private val startIcon = ViewUtilities iconSetter(Material.PLAY_ARROW, JavafxEnums.BIG_ICON)
  private val pauseIcon = ViewUtilities iconSetter(Material.PAUSE, JavafxEnums.BIG_ICON)
  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var toolbar: JFXToolbar = _
  @FXML protected var buttonStartPause: JFXButton = _
  @FXML protected var comboBoxShape: JFXComboBox[ShapeType.Value] = _
  @FXML protected var canvasPane: AnchorPane = _
  @FXML protected var canvas: Canvas = _
  protected var patchesControls: mutable.Map[(Int, Int), HBox] = _

  @FXML def initialize(): Unit = {
    this.assertNodeInjected()
    this.prepareButtons()
    this.prepareHideToolbar()
    this.prepareCombos()
    this.showPopupInfo()
    this.preparePatches()
  }

  private def showPopupInfo(): Unit = {
    ViewUtilities.showNotificationPopup("Help", "Click '^' and create a configuration \nRight Click on screen hides toolbar",
      JavafxEnums.LONG_DURATION, JavafxEnums.INFO_NOTIFICATION, null)
  }

  private def assertNodeInjected(): Unit = {
    assert(mainBorder != null, "fx:id=\"mainBorder\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(toolbar != null, "fx:id=\"toolbar\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(buttonStartPause != null, "fx:id=\"buttonStartPause\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(comboBoxShape != null, "fx:id=\"comboBoxShape\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(canvasPane != null, "fx:id=\"canvasPane\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(canvas != null, "fx:id=\"canvas\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  private def prepareButtons(): Unit = {
    this.buttonStartPause.setGraphic(this.startIcon)
    this.buttonStartPause setOnAction (_ => {
      val guardianLedbarBuilder: LedBargraphBuilder[_] = LedBargraphBuilder.create()
      guardianLedbarBuilder.noOfLeds(5)
      guardianLedbarBuilder
      this.patchesControls((0, 0)).getChildren.remove(1)
      this.patchesControls((0, 0)).getChildren.add(1, guardianLedbarBuilder.build())
      this.buttonStartPause.getGraphic match {
        case this.pauseIcon => this.buttonStartPause setGraphic this.startIcon
        case this.startIcon => this.buttonStartPause setGraphic this.pauseIcon
      }
    })
  }

  private def prepareCombos(): Unit = {
    ShapeType.values.foreach(this.comboBoxShape.getItems.add(_))
  }

  def preparePatches(): Unit = {
    this.patchesControls = PatchControlFactory.makeControlsBox(this.canvasPane, (2, 3), e => {
      val led = e.getSource.asInstanceOf[LedId]
      led.setOn(false)
    })
  }

  private def prepareHideToolbar(): Unit = {
    this.mainBorder.setOnMouseClicked(ev => {
      if (ev.getButton == MouseButton.SECONDARY && this.toolbar.isVisible) {
        this.toolbar setVisible false
      } else if (ev.getButton == MouseButton.SECONDARY && !this.toolbar.isVisible) {
        this.toolbar setVisible true
      }
    })
  }

  def log(message: String): Unit
  def startSimulation(): Unit
  def stopSimulation(): Unit
}

object Constants {
  val DEFAULT_SHAPE_POLYGON = 20
  val MAX_SHAPE_POLYGON = 100
  val MIN_SHAPE_POLYGON = 1
  val ENVIRONMENT_DEPTH = 1000
}