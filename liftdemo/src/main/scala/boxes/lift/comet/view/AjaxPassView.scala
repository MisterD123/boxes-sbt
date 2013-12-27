package boxes.lift.comet.view

import net.liftweb._
import http._
import SHtml._
import net.liftweb.common._
import net.liftweb.common.Box._
import net.liftweb.util._
import net.liftweb.actor._
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JsCmds.{SetHtml}
import boxes.View
import scala.xml.Text
import boxes.Var
import boxes.Path
import boxes.Val
import boxes.Ref
import scala.xml.NodeSeq
import boxes.BoxImplicits._
import boxes.Cal
import boxes.util.NumericClass
import boxes.persistence.mongo.MongoBox
import boxes.persistence.ClassAliases
import com.mongodb.casbah.commons.Imports._
import boxes.list.ListRef
import boxes.lift.user.PassHash
import boxes.Box
import net.liftweb.http.js.JsCmds._
import net.liftweb.common.Loggable
import net.liftweb.http.js.JsCmd
import boxes.Reaction
import net.liftweb.http.js.JE
import scala.language.implicitConversions
import boxes.lift.user.User
import boxes.lift.comet.AjaxView

sealed trait PassViewMode

/**
 * Supports creating a password if we have none, or editing any existing password, repeatedly if required
 */
case object PassEditing extends PassViewMode

/**
 * Supports only creating a password, once the password is created a message is displayed stating this has been successful
 */
case object PassCreation extends PassViewMode

/**
 * Supports only resetting the password. The existing password is not required. Once the password is reset a message is
 * displayed stating this has been successful
 */
case object PassReset extends PassViewMode

class AjaxPassView(user: Ref[User], mode: PassViewMode, valid: (String) => Option[String]) extends AjaxView with Loggable {
  var oldPass: Option[String] = None
  var newPassA: Option[String] = None
  var newPassB: Option[String] = None
  
  lazy val id = "passview_" + net.liftweb.util.Helpers.nextFuncName

  //FIXME use resources for strings
  def formSubmit() {
    boxes.Box.transact{
      for {
        ph <- if (mode == PassEditing) user().passHash() else Some(PassHash(""))
        old <- if (mode == PassEditing) oldPass else Some("")
        a <- newPassA
        b <- newPassB
      } {
        if (a != b) {
          S.error("New password and repeat do not match.")
        } else if (mode==PassEditing && !ph.checkPass(old)) {
          S.error("Incorrect current password.")
        } else if (mode==PassCreation && !user().passHash().isEmpty) {
          S.error("Password is already set.")
        } else if (valid(a).isDefined) {
          S.error(valid(a).getOrElse("Invalid new password."))
        } else {
          user().passHash() = Some(PassHash(a))
          mode match {
            case PassCreation => S.notice("Password created.")
            case PassEditing => S.notice("Password changed.")
            case PassReset => {
              //Each reset token can only be used once
              user().clearResetPasswordToken()
              //User email is validated now, since it must have been used to trigger reset
              user().validated() = true
              user().clearValidationToken()              
              S.notice("Password reset, you are now logged in.")
//              //Log them in
//              User.logInFreshSession(user())
            }
          }
        }
      }      
    }
  }

  def passwordLine(label: String, commit: (String) => Unit) = {
    (
      ("#label" #> (label + ":")) & 
      ("#control" #> SHtml.password("", commit(_)))
    ).apply(AjaxView.formRowXML)
  }
  
  override def partialUpdates = List(() => Replace(id, render))
 
  def renderForm() = 
    AjaxView.formWithId(
      (if (mode == PassEditing) passwordLine("Current password", s=>oldPass=Some(s)) else Nil) ++
      passwordLine("New password",     s=>newPassA=Some(s)) ++
      passwordLine("Repeat password",  s=>newPassB=Some(s)) ++
      (
        ("#label" #> ("")) & 
        ("#control" #> ajaxSubmit("Change password", ()=>formSubmit(), "class" -> "btn btn-primary"))
      ).apply(AjaxView.formRowXML),
      id
    )
    
  def renderMessage(m: String) = <div id={id} class="form-horizontal">{(
                 (".controls [class+]" #> "controls-text") & 
                 ("#label" #> "") & 
                 ("#control" #> m)
               ).apply(AjaxView.formRowXML)}</div>
  
  def render = {
    mode match {
      case PassCreation => if (user().passHash().isDefined) renderMessage(S.?("user.password.set")) else renderForm()
      case PassEditing => renderForm()
      case PassReset => renderForm()  //TODO after the user has reset the password, we should really prevent further editing, and if possible log the user in at "/"
    }
  }
}

object AjaxPassView {
  
  val isAlphabetic = (s: String) => s.filter(c => c.isLetter || c.isWhitespace).length == s.length
  val isNumeric = (s: String) => s.filter(c => c.isDigit).length == s.length
  
  val minLength = 8
  val maxLength = 512
  val minLengthAlphabetic = 16
  val minLengthNumeric = 16
  
  //FIXME strings as resources
  def defaultValidator(s: String) = {
    if (s.length() < minLength) {
      Some("Password must be at least " + minLength + " characters")
    } else if (s.length() > maxLength) {
      Some("Password must be at most " + maxLength + " characters")
    //Note that we don't want to tell anyone looking over user's shoulder
    //that their password is alphabetic, but we also don't want to allow
    //short passes without numbers or punctuation
    } else if (s.length() < minLengthAlphabetic && isAlphabetic(s)) {
      Some("Password must be at least" + minLengthAlphabetic + " characters")
    //Just numbers is probably a bad idea too, unless you have lots of them
    } else if (s.length() < minLengthNumeric && isNumeric(s)) {
      Some("Password must be at least" + minLengthNumeric + " characters")
    } else {
      None
    }
  }
  
  def apply(user: Ref[User], mode: PassViewMode = PassEditing, valid: (String) => Option[String] = defaultValidator) = new AjaxPassView(user, mode, valid)
}