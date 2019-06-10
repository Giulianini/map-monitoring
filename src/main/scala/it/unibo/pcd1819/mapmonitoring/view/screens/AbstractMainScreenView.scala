package it.unibo.pcd1819.mapmonitoring.view.screens

import com.jfoenix.controls._
import it.unibo.pcd1819.mapmonitoring.model.Environment
import it.unibo.pcd1819.mapmonitoring.model.Environment.{Coordinate, Patch}
import it.unibo.pcd1819.mapmonitoring.view.utilities._
import it.unibo.pcd1819.mapmonitoring.view.utilities.JavafxEnums.ShapeType
import javafx.fxml.FXML
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.input.MouseButton
import javafx.scene.layout.{AnchorPane, BorderPane, HBox}
import javafx.scene.paint.Color

import scala.collection.JavaConverters._
import scala.collection.mutable

trait View {
  def endAlert(patch: Patch): Unit
  def log(message: String): Unit
}

protected abstract class AbstractMainScreenView() extends View {
  private val _sensorPositions: mutable.Map[String, Coordinate] = mutable.HashMap()
  private var _patchesControls: mutable.Map[String, (LedPatch, HBox)] = _
  private var _context: GraphicsContext = _

  @FXML protected var mainBorder: BorderPane = _
  @FXML protected var toolbar: JFXToolbar = _
  @FXML protected var comboBoxShape: JFXComboBox[ShapeType.Value] = _
  @FXML protected var sliderDimension: JFXSlider = _
  @FXML protected var canvasPane: AnchorPane = _
  @FXML protected var canvas: Canvas = _

  protected val squareDrawing: Coordinate => Unit = c => context.fillRect(c.x - sliderDimension.getValue / 2, c.y - sliderDimension.getValue / 2,
    sliderDimension.getValue, sliderDimension.getValue)
  protected val circleDrawing: Coordinate => Unit = c => context.fillOval(c.x - sliderDimension.getValue / 2, c.y - sliderDimension.getValue / 2,
    sliderDimension.getValue, sliderDimension.getValue)
  protected var shapeDrawingConsumer: Coordinate => Unit = circleDrawing

  @FXML def initialize(): Unit = {
    this.initCanvas()
    this.assertNodeInjected()
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
    assert(sliderDimension != null, "fx:id=\"sliderDimension\" was not injected: check your FXML file 'MainScreen.fxml'.")
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
    this._context = canvas.getGraphicsContext2D
    this._context.setFill(Color.SLATEGREY)
  }

  private def prepareCombos(): Unit = {
    ShapeType.values.foreach(this.comboBoxShape.getItems.add(_))
    this.comboBoxShape.getSelectionModel.selectedItemProperty()
      .addListener((_, _, newValue) => newValue match {
        case ShapeType.CIRCLE => shapeDrawingConsumer = circleDrawing
        case ShapeType.SQUARE => shapeDrawingConsumer = squareDrawing
      })
  }

  def preparePatches(): Unit = {
    this._patchesControls = PatchControlFactory.makeControlsBox(this.canvasPane, e => {
      val led = e.getSource.asInstanceOf[LedPatch]
      if (led.isOn()) {
        led.setOn(false)
        this.endAlert(Environment.toPatch(led.name).get)
      }
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

  protected def guardianExists(guardianName: String): Boolean = this._patchesControls.values
    .flatMap(p => p._2.getChildren.asScala)
    .map(k => k.asInstanceOf[LedGuardian])
    .exists(g => g.name == guardianName)

  //GETTER
  protected def context: GraphicsContext = _context
  protected def patchesControls: mutable.Map[String, (LedPatch, HBox)] = this._patchesControls
  protected def sensorPositions: mutable.Map[String, Coordinate] = _sensorPositions
  //TO ACTOR
  override def endAlert(patch: Patch): Unit
  override def log(message: String): Unit
}

object Constants {
  val DEFAULT_SHAPE_POLYGON = 20
  val MAX_SHAPE_POLYGON = 100
  val MIN_SHAPE_POLYGON = 1
  val ENVIRONMENT_DEPTH = 1000
  var SENSOR_RADIUS = 10
}