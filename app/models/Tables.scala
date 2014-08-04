package models

import java.sql.Timestamp
import scala.slick.driver.MySQLDriver.simple._
import java.nio.file.Path
import java.nio.file.Paths
import scala.slick.model.ForeignKey
import scala.slick.model.ForeignKeyAction

object Tables {
  
  case class User(email: String, password: String, timestamp: Timestamp, id: Option[Long] = None)
  
  class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def email = column[String]("EMAIL", O.NotNull)
    def password = column[String]("PASSWORD", O.NotNull)
    def timestamp = column[Timestamp]("TIMESTAMP", O.NotNull)
    def id = column[Long]("ID", O.NotNull, O.PrimaryKey, O.AutoInc)
    def * = (email, password, timestamp, id.?) <> (User.tupled, User.unapply)
  }
  
  case class UserKey(userId: Long, key: String, timestamp: Timestamp, id: Option[Long] = None)
  
  class UserKeys(tag: Tag) extends Table[UserKey](tag, "USER_KEYS") {
    def userId = column[Long]("USER_ID", O.NotNull)
    def key = column[String]("KEY", O.NotNull)
    def timestamp = column[Timestamp]("TIMESTAMP", O.NotNull)
    def id = column[Long]("ID", O.NotNull, O.PrimaryKey, O.AutoInc)
    def * = (userId, key, timestamp, id.?) <> (UserKey.tupled, UserKey.unapply)
    def userIdFk = foreignKey("USER_KEYS_USER_ID_FK", userId, TableQuery[Users])(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
  }
}