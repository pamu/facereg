package actors

import akka.actor.Actor
import play.api.Logger
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import scala.concurrent.duration._
import org.openimaj.image.processing.face.detection.HaarCascadeDetector

object ManagerActor {
  case class BootMaster(masterName: String, workerName: String)
}

class ManagerActor extends Actor {
  val masters = scala.collection.mutable.Map[String, ActorRef]()
  val system = ActorSystem(Contants.SYSTEM)
  
  import ManagerActor._
  def receive = {
    
    case BootMaster(masterName, workerName) => {
      if(masters contains masterName) {
        masters(masterName) ! MasterActor.BootWorker(workerName, sender)
      }else {
        val master = system.actorOf(Props[MasterActor], Contants.MASTER)
        masters +=((masterName, master))
        master ! MasterActor.BootWorker(workerName, sender)
      }
    }
    
    case _ => Logger.info(s"Unknown message in ${Contants.MANAGER} Actor")
  }
}