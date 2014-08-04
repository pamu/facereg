package global

import play.api.GlobalSettings
import play.api.Application
import play.api.Logger
import models.DAO

object Global extends GlobalSettings {
  
  override def onStart(app: Application): Unit = {
    super.onStart(app)
    Logger.info("Face Recognition Service Started")
    //DAO.init
  }
  
  override def onStop(app: Application): Unit = {
    super.onStop(app)
    Logger.info("Face Recognition Service Stopped")
  }
}