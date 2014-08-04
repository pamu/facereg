package actors

import akka.actor.Actor
import akka.actor.ActorRef
import play.api.Logger
import akka.actor.ActorSystem
import akka.actor.Props

object MasterActor {
  case class BootWorker(workerName: String, senderActor: ActorRef)
}

class MasterActor extends Actor {
  val workers = scala.collection.mutable.Map[String, ActorRef]()
  val system = ActorSystem(Contants.SYSTEM)
  
  import MasterActor._
  def receive = {
    
    case BootWorker(workerName: String, senderActor) => {
      if(workers contains workerName) {
        senderActor ! workers(workerName)
      }else {
        val worker = system.actorOf(Props[WorkerActor], Contants.WORKER)
        workers += ((workerName, worker))
        senderActor ! worker
      }
    }
    case _ => Logger.info(s"Unknown message in ${Contants.MASTER} Actor")
  }
}