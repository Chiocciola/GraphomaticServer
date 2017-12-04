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
  var apiV1Calls = collection.mutable.Map[String, Long]().withDefaultValue(0)
  var apiV2Calls = collection.mutable.Map[String, Long]().withDefaultValue(0)
  var apiV3Calls = collection.mutable.Map[String, Long]().withDefaultValue(0)

  val icons = Map(
   "clear-day"           -> 1,
   "clear-night"         -> 2,
   "wind"                -> 3,
   "fog"                 -> 4,
   "partly-cloudy-day"   -> 5,
   "partly-cloudy-night" -> 6,
   "cloudy"              -> 7,
   "rain"                -> 8,
   "sleet"               -> 9,
   "snow"                -> 10
  ).withDefaultValue(0)



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

  def extractForecast2(darkSkyResponse: WSResponse) : (String, List[Long], List[Int], List[Int]) =
  {
    if (darkSkyResponse.status != 200)
    {
      return ("DarkSkyFail:" + darkSkyResponse.status, List[Long](), List[Int](), List[Int]())
    }

    Json.fromJson[DarkSkyResponse](darkSkyResponse.json) match
    {
      case r: JsSuccess[DarkSkyResponse] => ("",                           r.get.hourly.data.take(10) map (p => p.time.toLong), r.get.hourly.data.take(10) map (p => icons(p.icon)), r.get.hourly.data.take(10) map (p => (p.temperature + 0.5).toInt))
      case e: JsError =>                    ("DarkSkyFail:JsonValidation", List[Long](),                                        List[Int](),                                         List[Int]())
    }
  }

  def extractForecast3(darkSkyResponse: WSResponse) : (String, List[Long], List[Long], List[Long]) =
  {
    if (darkSkyResponse.status != 200)
    {
      return ("DarkSkyFail:" + darkSkyResponse.status, List[Long](), List[Long](), List[Long]())
    }

    Json.fromJson[DarkSkyResponse](darkSkyResponse.json) match
    {
      case r: JsSuccess[DarkSkyResponse] => ("",                           r.get.hourly.data.take(10) map (p => p.time.toLong), r.get.hourly.data.take(10) map (p => icons(p.icon).toLong), r.get.hourly.data.take(10) map (p => (p.temperature + 0.5).toLong))
      case e: JsError =>                    ("DarkSkyFail:JsonValidation", List[Long](),                                        List[Long](),                                        List[Long]())
    }
  }

  def index(latlng: String, darkskyapikey: String) = Action.async
  {
  	apiV1Calls(darkskyapikey) += 1

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

  def index2(latlng: String, darkskyapikey: String) = Action.async
  {
    apiV2Calls(darkskyapikey) += 1

    var darkSkyApiKeySafe = darkskyapikey.filter(_.isLetterOrDigit)
    var googleApiKey = sys.env("googleapikey")

    implicit request: Request[AnyContent] =>
    {
      val urlDarkSky = s"https://api.darksky.net/forecast/$darkSkyApiKeySafe/$latlng?exclude=minutely,daily,alerts,flags&units=auto"
      val urlGoogle  = s"https://maps.googleapis.com/maps/api/geocode/json?key=$googleApiKey&result_type=political&latlng=$latlng"

      for {
        (status, hours, icons, temp) <- ws.url(urlDarkSky).get() map extractForecast2
        location                     <- cache.getOrElseUpdate(latlng)(ws.url(urlGoogle).get() map extractLocation)
      } yield Ok(Json.toJson(new Graphomatic2Response(status, location, hours, icons, temp)))
    }
  }

  def index3(latlng: String, k: String) = Action.async
  {
    apiV3Calls(k) += 1

    var darkSkyApiKeySafe = k.filter(_.isLetterOrDigit)

    if (darkSkyApiKeySafe == "")
    {
      darkSkyApiKeySafe = sys.env("darkskyapikey")
    }

    var googleApiKey = sys.env("googleapikey")

    implicit request: Request[AnyContent] =>
    {
      val urlDarkSky = s"https://api.darksky.net/forecast/$darkSkyApiKeySafe/$latlng?exclude=minutely,daily,alerts,flags&units=auto"
      val urlGoogle  = s"https://maps.googleapis.com/maps/api/geocode/json?key=$googleApiKey&result_type=political&latlng=$latlng"

      for {
        (status, hours, icons, temp) <- ws.url(urlDarkSky).get() map extractForecast3
        location                     <- cache.getOrElseUpdate(latlng)(ws.url(urlGoogle).get() map extractLocation)
      } yield Ok(Json.toJson(new Graphomatic3Response(status, location, List(hours, icons, temp).transpose.flatten)))
    }
  }

  def stat() = Action
  {
	   implicit request: Request[AnyContent] => Ok(views.html.stat(apiV1Calls, apiV2Calls))
  }
}
