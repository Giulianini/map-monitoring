package it.unibo.pcd1819.mapmonitoring.view.screens

import akka.actor.ActorRef
import it.unibo.pcd1819.mapmonitoring.view.FXMLScreens
import it.unibo.pcd1819.mapmonitoring.view.utilities.ViewUtilities
import it.unibo.pcd1819.mapmonitoring.view.utilities.ViewUtilities._
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

trait ActorObserver {
  def setViewActorRef(actorRef: ActorRef): Unit
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
  override def log(message: String): Unit = log("log")
  override def startSimulation(): Unit = log("start")
  override def stopSimulation(): Unit = log("pause")
  override def setViewActorRef(actorRef: ActorRef): Unit = this.viewActorRef = actorRef


  // ##################### FROM VIEW ACTOR
}

object MainScreenView {
  def apply(): MainScreenView = new MainScreenView()
}