package controllers

import javax.inject._
import java.time._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._
import play.api.cache._

import play.api.Logger

import DarkSky._
import Google._
import Graphomatic._

@Singleton
class DataController @Inject()(cc: ControllerComponents, ws: WSClient, cache: AsyncCacheApi) extends AbstractController(cc)
{
  def extractLocation(googleResponse: WSResponse) : String =
  {
    if (googleResponse.status != 200)
    {
      return "GoogleFail:" + googleResponse.status;
    }

    Json.fromJson[GoogleResponse](googleResponse.json) match
    {
        case r: JsSuccess[GoogleResponse] => r.get.results.lift(0) flatMap (_.address_components.lift(0)) map (_.long_name)  getOrElse "n/a"
        case e: JsError => "GoogleFail:JsonValidation"
    }
  }

  def extractForecast(darkSkyResponse: WSResponse) : (String, List[GraphomaticPoint]) =
  {
    if (darkSkyResponse.status != 200)
    {
      return ("DarkSkyFail:" + darkSkyResponse.status, List[GraphomaticPoint]())
    }

    Json.fromJson[DarkSkyResponse](darkSkyResponse.json) match
    {
      case r: JsSuccess[DarkSkyResponse] => ("", r.get.hourly.data.take(10) map (p => new GraphomaticPoint(p.time, p.icon, (p.temperature + 0.5).toInt)))
      case e: JsError =>                    ("DarkSkyFail:JsonValidation", List[GraphomaticPoint]())
    }
  }

  def index(latlng: String, darkskyapikey: String) = Action.async
  {
    var darkSkyApiKeySafe = darkskyapikey.filter(_.isLetterOrDigit)
    var googleApiKey = sys.env("googleapikey")

    implicit request: Request[AnyContent] =>
    {
      val urlDarkSky = s"https://api.darksky.net/forecast/$darkSkyApiKeySafe/$latlng?exclude=minutely,daily,alerts,flags&units=auto"
      val urlGoogle  = s"https://maps.googleapis.com/maps/api/geocode/json?key=$googleApiKey&result_type=political&latlng=$latlng"

      for {
        (status, data) <- ws.url(urlDarkSky).get() map extractForecast
        location       <- cache.getOrElseUpdate(latlng)(ws.url(urlGoogle).get() map extractLocation)
      } yield Ok(Json.toJson(new GraphomaticResponse(status, location, data)))
    }
  }
}
