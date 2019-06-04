package it.unibo.pcd1819.mapmonitoring.view.screens

import akka.actor.ActorRef
import akka.cluster.Member
import com.sun.javafx.application.PlatformImpl
import it.unibo.pcd1819.mapmonitoring.model.Environment.{Coordinate, Patch}
import it.unibo.pcd1819.mapmonitoring.view.FXMLScreens
import it.unibo.pcd1819.mapmonitoring.view.utilities.ViewUtilities
import it.unibo.pcd1819.mapmonitoring.view.utilities.ViewUtilities._
import it.unibo.pcd1819.mapmonitoring.view.DashboardActor.{EndAlert, Log}
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

trait ActorObserver {
  def setViewActorRef(actorRef: ActorRef): Unit
  def sensorPosition(sensorId: String, c: Coordinate): Unit
  def guardianExistence(member: Member, guardianId: String, patch: Patch): Unit
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
  override def sensorPosition(sensorId: String, c: Coordinate): Unit = ???
  override def guardianExistence(member: Member, guardianId: String, patch: Patch): Unit = ???
  override def preAlert(guardianId: String): Unit = ???
  override def preAlertEnd(guardianId: String): Unit = ???
  override def alert(patch: Patch): Unit = ???
}

object MainScreenView {
  def apply(): MainScreenView = {
    PlatformImpl.startup(() => {})
    new MainScreenView()
  }
}