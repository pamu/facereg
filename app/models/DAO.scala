package models

import scala.slick.driver.MySQLDriver.simple._
import java.sql.Timestamp

object DAO {
  import Tables._
  val users = TableQuery[Users]
  val userKeys = TableQuery[UserKeys]
  
  def db = Database.forURL(url = "jdbc:mysql://localhost:3306/FaceRecognitionDB", driver = "com.mysql.jdbc.Driver", user = "root", password = "root")
  
  import Tables._
  def saveUser(user: User): Long = db.withTransaction(implicit tx => {
    val userAutoId = users returning users.map(_.id) into {
      case (_, id) => id
    }
    userAutoId.insert(user)
  })
  
  def saveUserKey(userKey: UserKey): Unit = db.withTransaction(implicit tx => {
    userKeys += userKey
  })
  
  def getCurrentTimestamp: Timestamp = {
    import java.util.Calendar
    val timestamp = new Timestamp(Calendar.getInstance.getTime.getTime)
    timestamp
  }
  
  def findOneWithEmailIfExists(email: String): Option[User] = db.withTransaction(implicit tx => {
    val userQuery = for(user <- users.filter(_.email === email)) yield user
    userQuery.firstOption
  })
  
  def getUserKey(userId: Long): Option[UserKey] = db.withTransaction(implicit tx => {
    val userKeyQuery = for(userKey <- userKeys.filter(_.userId === userId)) yield userKey
    userKeyQuery.firstOption
  })
  
  def checkUser(data: (String, String)): Boolean = db.withTransaction(implicit tx => {
    val checkUserQuery = for(user <- users.filter(_.email === data._1).filter(_.password === data._2)) yield user
    checkUserQuery.exists.run
  })
  
  def checkIfEmailTaken(email: String): Boolean = db.withTransaction(implicit tx => {
    val emailQuery = for(user <- users.filter(_.email === email)) yield user
    emailQuery.exists.run
  })
  
  def findOneWithKeyIfExists(key: String): Option[User] = db.withTransaction(implicit tx => {
    val keyQuery = for((userkey, user) <- userKeys.filter(_.key === key) innerJoin users on(_.userId === _.id)) yield user
    keyQuery.firstOption
  })
  
  def keyExists(key: String): Boolean = db.withTransaction(implicit tx => {
    val keyQuery = for(userKey <- userKeys.filter(_.key === key)) yield userKey
    keyQuery.exists.run
  })
}