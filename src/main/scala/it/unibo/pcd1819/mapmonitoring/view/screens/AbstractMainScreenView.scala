package it.unibo.pcd1819.mapmonitoring.view.screens

import java.util.stream.IntStream

import com.jfoenix.controls._
import it.unibo.pcd1819.mapmonitoring.view.utilities.{JavafxEnums, ViewUtilities}
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.{AnchorPane, BorderPane}
import org.kordamp.ikonli.material.Material

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
  @FXML protected var comboBoxOptimize: JFXComboBox[String] = _
  @FXML protected var canvasPane: AnchorPane = _
  @FXML protected var canvas: Canvas = _

  @FXML def initialize(): Unit = {
    this.assertNodeInjected()
    this.prepareButtons()
    this.prepareHideToolbar()
    this.prepareCombos()
    this.showPopupInfo()
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
    assert(comboBoxOptimize != null, "fx:id=\"comboBoxOptimize\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(canvasPane != null, "fx:id=\"canvasPane\" was not injected: check your FXML file 'MainScreen.fxml'.")
    assert(canvas != null, "fx:id=\"canvas\" was not injected: check your FXML file 'MainScreen.fxml'.")
  }

  private def prepareButtons(): Unit = {
    this.buttonStartPause.setGraphic(this.startIcon)
    this.buttonStartPause setOnAction (_ => {
      this.buttonStartPause.getGraphic match {
        case this.pauseIcon => this.buttonStartPause setGraphic this.startIcon
        case this.startIcon => this.buttonStartPause setGraphic this.pauseIcon
      }
    })
  }

  private def prepareCombos(): Unit = {
    ShapeType.values.foreach(this.comboBoxShape.getItems.add(_))
    this.comboBoxShape.getSelectionModel.select(1)
    IntStream.range(Constants.MIN_SHAPE_POLYGON, Constants.MAX_SHAPE_POLYGON).forEach({
      i => this.comboBoxOptimize.getItems.add(i + "Polygons")
    })
    this.comboBoxOptimize.getSelectionModel.select(Constants.DEFAULT_SHAPE_POLYGON)
  }

  private def prepareHideToolbar(): Unit = {
    this.mainBorder.setOnMouseClicked(ev => {
      if (ev.getButton == MouseButton.MIDDLE && this.toolbar.isVisible) {
        this.toolbar setVisible false
      } else if (ev.getButton == MouseButton.MIDDLE && !this.toolbar.isVisible) {
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