package it.unibo.pcd1819.mapmonitoring.view.screens

import com.jfoenix.controls._
import it.unibo.pcd1819.mapmonitoring.model.Environment
import it.unibo.pcd1819.mapmonitoring.view.utilities._
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.layout.{AnchorPane, BorderPane, HBox}
import org.kordamp.ikonli.material.Material

import scala.collection.JavaConverters._
import scala.collection.mutable

trait View {
  def startSimulation(): Unit
  def stopSimulation(): Unit
}

protected abstract class AbstractMainScreenView() extends View {
  private val startIcon = ViewUtilities iconSetter(Material.PLAY_ARROW, JavafxEnums.BIG_ICON)
  private val pauseIcon = ViewUtilities iconSetter(Material.PAUSE, JavafxEnums.BIG_ICON)
  private var _patchesControls: mutable.Map[String, (LedPatch, HBox)] = _

  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var toolbar: JFXToolbar = _
  @FXML protected var buttonStartPause: JFXButton = _
  @FXML protected var comboBoxShape: JFXComboBox[ShapeType.Value] = _
  @FXML protected var canvasPane: AnchorPane = _
  @FXML protected var canvas: Canvas = _

  @FXML def initialize(): Unit = {
    this.initCanvas()
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

  def initCanvas(): Unit = {
    this.canvasPane.setMinWidth(Environment.width)
    this.canvasPane.setMinHeight(Environment.height)
    this.canvasPane.setMaxWidth(Environment.width)
    this.canvasPane.setMaxHeight(Environment.height)
    this.canvas.setWidth(Environment.width)
    this.canvas.setHeight(Environment.height)
    PatchControlFactory.makeSeparator(this.canvasPane)
  }

  private def prepareButtons(): Unit = {
    this.buttonStartPause.setGraphic(this.startIcon)
    this.buttonStartPause setOnAction (_ => {
      this.addGuardian("A", "bombo")
      this.setGuardianBlinking("bombo", blinking = true)
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
    this._patchesControls = PatchControlFactory.makeControlsBox(this.canvasPane, e => {
      val led = e.getSource.asInstanceOf[LedPatch]
      led.setOn(!led.isOn)
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

  def patchesControls: mutable.Map[String, (LedPatch, HBox)] = this._patchesControls
  def addGuardian(patchName: String, guardianName: String): Unit = {
    if (!guardianExists(guardianName)) this._patchesControls(patchName)._2.getChildren.add(LedGuardian(guardianName))
  }
  def setPatchBlinking(patchName: String, blinking: Boolean): Unit = this._patchesControls(patchName)._1.setBlinking(blinking)
  def setGuardianBlinking(guardianName: String, blinking: Boolean): Unit = {
    val guardian = this._patchesControls.values
      .flatMap(p => p._2.getChildren.asScala)
      .map(k => k.asInstanceOf[LedGuardian])
      .find(g => g.name == guardianName)
    if (guardian.isDefined) guardian.get.setBlinking(blinking) else
      ViewUtilities.showNotificationPopup("Error", "Guardian not exists", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
  }

  private def guardianExists(guardianName: String): Boolean = this._patchesControls.values
    .flatMap(p => p._2.getChildren.asScala)
    .map(k => k.asInstanceOf[LedGuardian])
    .exists(g => g.name == guardianName)

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