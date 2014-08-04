package controllers

import play.api.mvc.RequestHeader
import play.api.mvc.Results
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Security
import play.api.mvc.Action
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.BodyParsers._
import models.Tables._
import models.DAO._
import play.api.libs.json.JsValue

trait FaceRecognitionSecured {
  def registered(request: RequestHeader) = request.headers.get("user_key")
  
  def onUnauthorized(request: RequestHeader) = Results.Unauthorized(Json.obj("failure" -> "not a registered user")).as("text/json")
  
  
  private def withUserKey(f: => String => Request[JsValue] => Future[Result]) = {
    Security.Authenticated(registered, onUnauthorized) { userKey =>
      Action.async(parse.json(maxLength = 5 * 1024 * 1024))(request => f(userKey)(request))
    }
  }
  
  def withUser(f: User => Request[JsValue] => Future[Result]) = withUserKey { userKey => request =>
    findOneWithKeyIfExists(userKey).map(user => f(user)(request)).getOrElse(Future(onUnauthorized(request)))
  }
}