package controllers

import javax.inject._
import java.time._

import scala.concurrent._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits._

import DarkSky._
import Google._

@Singleton
class DataController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc)
{
//  def index() = Action {
//	implicit request: Request[AnyContent] => Ok(views.html.index())
//  }

  implicit val dataPointReads = Json.reads[DataPoint]
  implicit val hourlyReads = Json.reads[Hourly]
  implicit val darkSkyResponseReads = Json.reads[DarkSkyResponse]

  implicit val googleAddressReads = Json.reads[GoogleAddress]
  implicit val googleCoordinatesReads = Json.reads[GoogleCoordinates]
  implicit val googleBoundsReads = Json.reads[GoogleBounds]
  implicit val googleGeometryReads = Json.reads[GoogleGeometry]
  implicit val googleResultReads = Json.reads[GoogleResult]
  implicit val googleResponseReads = Json.reads[GoogleResponse]


  case class GraphomaticPoint(
    h: Long,
    i: String,
    t: Int
  )

  case class GraphomaticResponse(
    stat: String,
    location: String,
    hourly: List[GraphomaticPoint]
  )

  implicit val gaphomaticPoitWrites = Json.writes[GraphomaticPoint]
  implicit val graphomaticResponseWritess = Json.writes[GraphomaticResponse]

  def adjustUnixTime(unixTime: Long, zone: ZoneId ) : Long =
  {
    val offset = zone.getRules().getOffset(Instant.ofEpochSecond(unixTime))

    LocalDateTime.ofEpochSecond(unixTime, 0, offset).toEpochSecond(ZoneOffset.UTC)
  }

  def getResponse(darkSkyResponse: WSResponse, googleResponse: WSResponse) : Result = 
  {
    var status = ""
    var location = "XXX"
    var data= List[GraphomaticPoint]()

    if (darkSkyResponse.status == 200)
    {
      Json.fromJson[DarkSkyResponse](darkSkyResponse.json) match {
        case r: JsSuccess[DarkSkyResponse] => data = r.get.hourly.data.take(10)map(point => new GraphomaticPoint(adjustUnixTime(point.time, ZoneId.of(r.get.timezone)), point.icon, (point.temperature + 0.5).toInt))
        case e: JsError =>                    status = "DarkSkyFail:JsonValidation"
      }
    }
    else
    {
      status = "DarkSkyFail:" + darkSkyResponse.status;
    }

    if (googleResponse.status == 200)
    {
      Json.fromJson[GoogleResponse](googleResponse.json) match {
        case r: JsSuccess[GoogleResponse] => location = r.get.results(0).address_components(0).long_name
        case e: JsError => status = "GoogleFail:JsonValidation"
      }
    }
    else
    {
      status = "GoogleFail:" + googleResponse.status;
    }


    Ok(Json.toJson(new GraphomaticResponse(status, location, data)))
  }

  def index() = Action.async
  {
    implicit request: Request[AnyContent] =>
    {
      val params = request.queryString.map { case(k,v) => k->v.mkString}

      //var latlng = "45.671837,12.324886"
      //var latlng = "55.073965,82.909996"
      var latlng = params("latlng")
      var darkskyapikey = params("darkskyapikey")

      val urlDarkSky = "https://api.darksky.net/forecast/" + darkskyapikey + "/" + latlng + "?exclude=minutely,daily,alerts,flags&units=auto"
      val urlGoogle  = "https://maps.googleapis.com/maps/api/geocode/json?key=" + sys.env("googleapikey") + "&result_type=political&latlng=" + latlng

      for {
        darkSky <- ws.url(urlDarkSky).get()
        google  <- ws.url(urlGoogle).get()
      } yield getResponse(darkSky, google)
    }
  }
}