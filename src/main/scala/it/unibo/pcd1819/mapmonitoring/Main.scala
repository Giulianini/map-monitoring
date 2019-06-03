package it.unibo.pcd1819.mapmonitoring

import com.sun.javafx.application.PlatformImpl
import it.unibo.pcd1819.mapmonitoring.view.screens.{MainScreenView, View}

object Main extends App {
  PlatformImpl.startup(() => {})
  var view: View = MainScreenView()
}
