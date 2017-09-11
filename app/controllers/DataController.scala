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
    Logger.debug("Handle Google reponse")

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

  def getResponse(darkSkyResponse: WSResponse, location: String) : Result =
  {
    var status = ""
    var data= List[GraphomaticPoint]()

    if (darkSkyResponse.status == 200)
    {
      Json.fromJson[DarkSkyResponse](darkSkyResponse.json) match {
        case r: JsSuccess[DarkSkyResponse] => data = r.get.hourly.data.take(10) map (point => new GraphomaticPoint(point.time, point.icon, (point.temperature + 0.5).toInt))
        case e: JsError =>                    status = "DarkSkyFail:JsonValidation"
      }
    }
    else
    {
      status = "DarkSkyFail:" + darkSkyResponse.status;
    }

    Ok(Json.toJson(new GraphomaticResponse(status, location, data)))
  }

  def index(latlng: String, darkskyapikey: String) = Action.async
  {
    implicit request: Request[AnyContent] =>
    {
      val urlDarkSky = "https://api.darksky.net/forecast/" + darkskyapikey + "/" + latlng + "?exclude=minutely,daily,alerts,flags&units=auto"
      val urlGoogle  = "https://maps.googleapis.com/maps/api/geocode/json?key=" + sys.env("googleapikey") + "&result_type=political&latlng=" + latlng

      for {
        darkSky <- ws.url(urlDarkSky).get()
        location  <- cache.getOrElseUpdate(latlng)(ws.url(urlGoogle).get().map(extractLocation))
      } yield getResponse(darkSky, location)
    }
  }
}
