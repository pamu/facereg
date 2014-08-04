package controllers

import play.api.mvc.{Action, Controller}
import akka.actor.ActorSystem
import actors.Contants
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import actors._
import play.api.libs.json.Json
import play.api.libs.json.Reads
import org.openimaj.image.FImage
import java.io.ByteArrayInputStream
import org.openimaj.image.processing.face.detection.DetectedFace
import org.apache.commons.codec.binary.Base64
import org.openimaj.image.ImageUtilities
import org.openimaj.image.processing.face.detection.HaarCascadeDetector
import scala.collection.JavaConverters._
import org.openimaj.image.processing.face.detection.keypoints.FKEFaceDetector
import org.openimaj.image.processing.face.detection.keypoints.KEDetectedFace
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsPath
import play.api.libs.functional.syntax._
import models.DAO._
import play.api.mvc.Results
import play.api.mvc.Result
import akka.actor.ActorRef


object FaceRecognition extends Controller with FaceRecognitionSecured {
  val sys = ActorSystem(Contants.SYSTEM)
  val manager = sys actorOf(Props[ManagerActor], Contants.MANAGER)
  val faceDetector: FKEFaceDetector = new FKEFaceDetector(new HaarCascadeDetector)
  
  case class TrainData(subjectName: String, galleryName: String, image: String)
  
  implicit val trainDataReads: Reads[TrainData] = (
      (JsPath \ "subjectName").read[String] and
      (JsPath \ "galleryName").read[String] and
      (JsPath \ "image").read[String]
  )(TrainData.apply _)
  
  def train = withUser { user => request =>
    request.body.validate[TrainData].fold(invalid = {
      errors => {
        Future(BadRequest(Json.obj("failure" -> "not a valid json format")))
      }
    }, valid = {
      data => {
        val faceFuture = Future {
          val bigFace = for(image <- decodeImage(data.image); face <- getLargestFace(image)) yield face
          bigFace
        }
        implicit val timeout = Timeout(500 milliseconds)
        val worker = (manager ? ManagerActor.BootMaster(user.email, data.galleryName)).mapTo[ActorRef]
        
        faceFuture.flatMap(face => {
          worker.map(actor => {
            face match {
              case Success(face) => {
                actor ! WorkerActor.Train(data.subjectName, face)
                Ok(Json.obj("success" -> "training successful"))
              }
              case Failure(t) => Ok(Json.obj("failure" -> t.getLocalizedMessage()))
            }
           
          }).recover{
            case t => Ok(Json.obj("failure" -> t.getLocalizedMessage()))
          }
        })
      }//data
    })
  }
  
  case class SmartTrainData(subjectName: String, galleryName: String, image: String)
  
  implicit val smartTrainDataReads: Reads[SmartTrainData] = (
      (JsPath \ "subjectName").read[String] and
      (JsPath \ "galleryName").read[String] and
      (JsPath \ "image").read[String]
  )(SmartTrainData.apply _)
  
  def smartTrain = withUser { user => request =>
    request.body.validate[SmartTrainData].fold(invalid = {
      errors => Future(BadRequest(Json.obj("failure" -> "not a valid json format")))
    }, valid = {
      data => {
        val faceFuture = Future {
          val bigFace = for(image <- decodeImage(data.image); face <- getLargestFace(image)) yield face
          bigFace
        }
        implicit val timeout = Timeout(500 milliseconds)
        val worker = (manager ? ManagerActor.BootMaster(user.email, data.galleryName)).mapTo[ActorRef]
        
        faceFuture.flatMap(face => {
          worker.map(actor => {
            face match {
              case Success(face) => {
                actor ! WorkerActor.SmartTrain(data.subjectName, face)
                Ok(Json.obj("success" -> "training successful"))
              }
              case Failure(t) => Ok(Json.obj("failure" -> t.getMessage()))
            }
            
          }).recover{
            case t => Ok(Json.obj("failure" -> t.getMessage()))
          }
        })
      }
    })
  }
  
  case class RecognizeData(galleryName: String, image: String)
  
  implicit val recognizeDataReads: Reads[RecognizeData] = (
     (JsPath \ "galleryName").read[String] and
     (JsPath \ "image").read[String]
   )(RecognizeData.apply _)
   
  def recognize = withUser { user => request =>
    request.body.validate[RecognizeData].fold(invalid = {
      errors => Future(BadRequest(Json.obj("failure" -> "bad json format")))
    }, valid = {
      data => {
        val faceFuture = Future {
          val bigFace = for(image <- decodeImage(data.image); face <- getLargestFace(image)) yield face
          bigFace.get
        }
        val timeout = Timeout(1 seconds)
        val worker = (manager.ask(ManagerActor.BootMaster(user.email, data.galleryName))(timeout)).mapTo[ActorRef]
        faceFuture.flatMap(face => {
          worker.flatMap(actor => {
        	implicit val newtimeout = Timeout(8 seconds)
            val recognize = (actor ? WorkerActor.Recognize(face)).mapTo[Try[(String, Float)]]
            recognize.map(result => result.get).map(t => Ok(Json.obj("success" -> "match found", "subjectName" -> t._1, "confidence" -> t._2.toString)))
          })
        }).recover{case t => Ok(Json.obj("failure" -> t.getMessage))}
      }
    })
  }
  
  def decodeImage(imageString: String): Try[FImage] = {
    if(Base64.isBase64(imageString)) {
      val imageBytes = Base64.decodeBase64(imageString)
      val inStream = new ByteArrayInputStream(imageBytes)
      val image: FImage = ImageUtilities.readF(inStream)
      Success(image)
    }else {
     Failure(new Exception("bad image format"))
    }
  }
  
  def getLargestFace(image: FImage): Try[KEDetectedFace] = {
    val faces = faceDetector.detectFaces(image)
    object FaceAreaOrdering extends Ordering[KEDetectedFace] {
      def compare(a: KEDetectedFace, b: KEDetectedFace) = a.getBounds().calculateArea() compare b.getBounds().calculateArea()
    }
    val bigFace: Try[KEDetectedFace] = if(faces.size > 0) Success(faces.asScala.max[KEDetectedFace](FaceAreaOrdering)) else Failure(new Exception("no faces found"))
    bigFace
  }
}