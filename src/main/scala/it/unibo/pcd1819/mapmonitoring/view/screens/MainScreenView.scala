package it.unibo.pcd1819.mapmonitoring.view.screens

import akka.actor.ActorRef
import com.sun.javafx.application.PlatformImpl
import it.unibo.pcd1819.mapmonitoring.model.Environment.{Coordinate, Patch}
import it.unibo.pcd1819.mapmonitoring.view.FXMLScreens
import it.unibo.pcd1819.mapmonitoring.view.utilities.{JavafxEnums, LedGuardian, ViewUtilities}
import it.unibo.pcd1819.mapmonitoring.view.utilities.ViewUtilities._
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor.{EndAlert, Log}
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

import scala.collection.JavaConverters._

trait ActorObserver {
  def setViewActorRef(actorRef: ActorRef): Unit
  def sensorPosition(sensorId: String, c: Coordinate): Unit
  def guardianExistence(guardianId: String, patch: Patch): Unit
  def preAlert(guardianId: String)
  def preAlertEnd(guardianId: String)
  def alert(patch: Patch)
}

final class MainScreenView extends AbstractMainScreenView() with ActorObserver {
  private var viewActorRef: ActorRef = _
  Platform.runLater(() => this.mainBorder = ViewUtilities.loadFxml(this, FXMLScreens.HOME).asInstanceOf[BorderPane])

  @FXML override def initialize(): Unit = {
    super.initialize()
    val stage = new Stage()
    val scene = new Scene(this.mainBorder)
    stage.setScene(scene)
    chargeSceneSheets(scene)
    stage.setOnCloseRequest(_ => {
      System.exit(0)
    })
    stage.show()
  }

  // ##################### TO VIEW ACTOR
  override def log(message: String): Unit = this.viewActorRef ! Log(message)
  override def endAlert(patch: Patch): Unit = this.viewActorRef ! EndAlert(patch)

  // ##################### FROM VIEW ACTOR
  override def setViewActorRef(actorRef: ActorRef): Unit = this.viewActorRef = actorRef
  override def guardianExistence(guardianId: String, patch: Patch): Unit = Platform.runLater(() => {
    if (!guardianExists(guardianId)) this.patchesControls(patch.name)._2.getChildren.add(LedGuardian(guardianId))
  })
  override def preAlert(guardianId: String): Unit = setGuardianOn(guardianId, on = true)
  override def preAlertEnd(guardianId: String): Unit = setGuardianOn(guardianId, on = false)
  override def alert(patch: Patch): Unit = setPatchBlinking(patch.name, blinking = true)
  override def sensorPosition(sensorId: String, c: Coordinate): Unit = {
    this.sensorPositions += (sensorId -> c)
    printSensorPositions()
  }

  //############################# METHODS ##################################
  private def setPatchBlinking(patchName: String, blinking: Boolean): Unit = Platform.runLater(() => {
    this.patchesControls(patchName)._1.setOn(blinking)
  })

  private def setGuardianOn(guardianName: String, on: Boolean): Unit = Platform.runLater(() => {
    val guardian = this.patchesControls.values
      .flatMap(p => p._2.getChildren.asScala)
      .map(k => k.asInstanceOf[LedGuardian])
      .find(g => g.name == guardianName)
    if (guardian.isDefined) guardian.get.setOn(on) else
      ViewUtilities.showNotificationPopup("Error", "Guardian not exists", JavafxEnums.MEDIUM_DURATION, JavafxEnums.ERROR_NOTIFICATION, null)
  })

  private def printSensorPositions(): Unit = Platform.runLater(() => {
    this.context.clearRect(0, 0, canvasPane.getWidth, canvasPane.getHeight)
    this.sensorPositions.values
      .filter(p => p.x < canvasPane.getWidth - sliderDimension.getValue / 2 && p.x > sliderDimension.getValue / 2
        && p.y < canvasPane.getHeight - sliderDimension.getValue / 2 && p.y > sliderDimension.getValue / 2)
      .foreach(shapeDrawingConsumer)
  })
}

object MainScreenView {
  def apply(): MainScreenView = {
    PlatformImpl.startup(() => {})
    new MainScreenView()
  }
}