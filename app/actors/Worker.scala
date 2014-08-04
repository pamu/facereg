package actors

import akka.actor.Actor
import org.openimaj.image.processing.face.detection.DetectedFace
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector
import org.openimaj.image.processing.face.recognition.EigenFaceRecogniser
import org.openimaj.image.processing.face.recognition.FaceRecognitionEngine
import org.openimaj.image.processing.face.detection.HaarCascadeDetector
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace
import org.openimaj.image.processing.face.alignment.RotateScaleAligner
import org.openimaj.feature.DoubleFVComparison
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import akka.pattern.pipe
import play.api.libs.json.JsObject
import play.api.Logger
import org.openimaj.image.processing.resize.ResizeProcessor
import org.openimaj.image.FImage
import scala.util.Success
import scala.util.Try
import scala.util.Failure
import org.openimaj.image.processing.face.recognition.FisherFaceRecogniser

object WorkerActor {
  case class Train(subjectName: String, face: KEDetectedFace)
  case object Train
  case class SmartTrain(subjectName: String, face: KEDetectedFace)
  case object SmartTrain
  case class Recognize(face: KEDetectedFace)
  case class RestrictSubject(subjectId: String)
  case class AllowSubject(subjectId: String)
  case object Reset
  case object ResetDone
  case object ListPeople
}

class WorkerActor extends Actor {
  
  val faceDetector: FKEFaceDetector = new FKEFaceDetector(new HaarCascadeDetector)
  val faceRecognizer: FisherFaceRecogniser[KEDetectedFace, String] = FisherFaceRecogniser.create(20, new RotateScaleAligner, 1, DoubleFVComparison.EUCLIDEAN, 0.9f)
  val faceEngine: FaceRecognitionEngine[KEDetectedFace, String] = FaceRecognitionEngine.create(faceDetector, faceRecognizer)
  
  val restricted: java.util.Set[String] = new java.util.HashSet[String]() 
  import WorkerActor._
  
  def receive = {
    
    case Train(subjectName, face) => {
      val f: Future[Train.type] = Future {
        faceEngine.train(face, subjectName)
        Train
      }
      f pipeTo self
    }
    case Train => Logger.info("training done")
    case SmartTrain(subjectName, face) => {
      val f: Future[SmartTrain.type] = Future {
        
        val area = face.getFacePatch().getContentArea().calculateArea()
       
        area match {
          case x if x > 2048 * 2048 => mFaceTrain((5 to 11).toList, face.getFacePatch(), subjectName)
          case x if (x > 1024 * 1024 && x < 2048 * 2048) => mFaceTrain((8 to 10).toList, face.getFacePatch(), subjectName)
          case x if (x > 512 * 512 && x < 1024 * 1024) => mFaceTrain((8 to 9).toList, face.getFacePatch(), subjectName)
          case x if (x > 128 * 128 && x < 512 * 512) => mFaceTrain((8 to 8).toList, face.getFacePatch(), subjectName)
          case x if x > 0 => Unit
        }
        mFaceTrain((5 to 7).toList, face.getFacePatch(), subjectName)
        faceEngine.train(face, subjectName)
        SmartTrain
      }
      f pipeTo self
    }
    case SmartTrain => Logger.info("training done (enchanced)")
    case f: akka.actor.Status.Failure => Logger.error("training operation failed, cause: "+f.cause)
    case Recognize(face) => {
      val f: Future[Try[(String, Float)]] = Future {
        if(faceEngine.getRecogniser().listPeople().size() > 0) {
           val list = faceEngine.recogniseBest(face.getFacePatch())
           if(list.size > 0) {
             val scored = list.get(0).getSecondObject()
             if(scored != null) {
               Success((scored.annotation, scored.confidence))
             }else {
               Failure(new RuntimeException("no match found"))
             }
           }else {
             Failure(new Exception("no match found."))
           }
        }else {
          Failure(new Exception("face recognition engine is not trained"))
        }
      }
      f pipeTo sender
    }
    
    case RestrictSubject(subjectName) => {
      if(!restricted.contains(subjectName)) restricted.add(subjectName)
      Logger.info("Restricted subject")
    }
    
    case AllowSubject(subjectName) => {
      if(restricted.contains(subjectName)) restricted.remove(subjectName)
      Logger.info("Allowed Subject")
    }
    
    case Reset => {
      val f = Future {
        faceEngine.getRecogniser().reset()
        ResetDone
      }
    }
    case ResetDone => Logger.info("Reset done")
    case x => Logger.info(s"Unknown message in ${Contants.WORKER} actor message type: ${x.getClass}")
  }
  
  def mFaceTrain(list: List[Int], face: FImage, subjectName: String): Unit = {
    for(x <- list) {
      faceEngine.train(subjectName, ResizeProcessor.resample(face, Math.pow(2, x).asInstanceOf[Int], Math.pow(2, x).asInstanceOf[Int]))
    }
  }
}