package controllers

import javax.inject._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._



@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc)
{
  def index() = Action {
	implicit request: Request[AnyContent] => Ok(views.html.index())
  }
}
