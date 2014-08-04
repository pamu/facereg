package controllers

import play.api.mvc.RequestHeader
import play.api.mvc.Security
import play.api.mvc.Results
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Result
import play.api.mvc.Action
import models.Tables._
import models.DAO._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait Secured {
  def userEmail(request: RequestHeader) = request.session.get("email")
  
  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(userEmail, onUnauthorized) { email =>
      Action.async (request => Future(f(email)(request)))
    }
  }
  
  def withUser(f: User => Request[AnyContent] => Result) = withAuth { email => request =>
    findOneWithEmailIfExists(email).map(user => f(user)(request)).getOrElse(onUnauthorized(request))
  } 
}