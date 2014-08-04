package controllers

import play.api.mvc.{Action, Controller}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import models.DAO._
import models.Tables._
import play.api.Logger
import scala.util.Random
import java.util.Calendar
import java.security.SecureRandom

object Application extends Controller with Secured {
  
  def index = Action {
    Redirect(routes.Application.login)
  }
  
  val loginForm = Form(
      tuple("Email" -> email,
          "Password" -> nonEmptyText
          ).verifying("Login failed !!!, Wrong Username or Password", data => checkUser(data))
      )
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }
  
  def loginPost = Action.async( implicit request =>
    Future {
      loginForm.bindFromRequest.fold(errors => {
        BadRequest(views.html.login(errors))
      }, 
      success => {
        Redirect(routes.Application.home).withSession("email" -> success._1)
      })
    }
  )
  
  val signupForm = Form(
      tuple("Email" -> email,
          "Password" -> nonEmptyText,
          "Retype-Password" -> nonEmptyText
          ).verifying("Password doesn't match !!!", data => data._2 == data._3).verifying("Signup Failed !!!, Email already taken  !!!", data => !checkIfEmailTaken(data._1))
      )
  def signup = Action { implicit request =>
    Ok(views.html.signup(signupForm))
  }
  
  def signupPost = Action.async( implicit request =>
    Future {
      signupForm.bindFromRequest.fold(errors => {
        BadRequest(views.html.signup(errors))
      }, success => {
        val userId = saveUser(User(success._1, success._2, getCurrentTimestamp))
        val randomString = getSecureString
        if(keyExists(randomString)) {
          val newRandomString = s"${randomString}${Calendar.getInstance().getTime().getTime()}"
          saveUserKey(UserKey(userId, newRandomString, getCurrentTimestamp))
        } else {
          saveUserKey(UserKey(userId, randomString, getCurrentTimestamp))
        }
        Redirect(routes.Application.signup).flashing("success" -> "Signup Successful !!!")
      })
    }
   )
   
  def home = withUser { user => request =>
    Ok(views.html.home(getUserKey(user.id.get)))
  }
  
  def logout = Action {
    Redirect(routes.Application.login).withNewSession
  }
  
  def getSecureString: String = {
    val random = new SecureRandom
    BigInt(130, random).toString(32)
  }
}