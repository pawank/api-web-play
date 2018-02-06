package controllers

import javax.inject.{ Inject, Singleton }

import models.User
import pdi.jwt.JwtSession._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class WebApplication @Inject()(scc: SecuredControllerComponents, assets: AssetsFinder)(implicit ec: ExecutionContext)
    extends SecuredController(scc) {

  private val passwords = Seq("red", "blue", "green")

  private val loginForm: Reads[(String, String)] =
    ((JsPath \ "username").read[String] and (JsPath \ "password").read[String]).tupled

  def index = Action {
    Ok(views.html.web(assets))
  }

  def login = Action(parse.json).async { implicit request: Request[JsValue] =>
    val result = request.body
      .validate(loginForm)
      .fold(
        errors => {
          BadRequest(JsError.toJson(errors))
        }, {
          case (username, password) =>
            if (passwords.contains(password))
              Ok.addingToJwtSession("user", User(username))
            else
              Unauthorized
        }
      )

    Future(result)
  }

  def publicApi = Action.async {
    Future(Ok("That was easy!"))
  }

  def privateApi = AuthenticatedAction.async {
    Future(Ok("Only the best can see that."))
  }

  def adminApi = AdminAction.async {
    Future(Ok("Top secret data. Hopefully, nobody will ever access it."))
  }

}
