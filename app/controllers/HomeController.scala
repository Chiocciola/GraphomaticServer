package controllers

import javax.inject._
import java.time._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits._

import DarkSky._

@Singleton
class HomeController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc)
{
//  def index() = Action {
//	implicit request: Request[AnyContent] => Ok(views.html.index())
//  }

  implicit val dataPointReads = Json.reads[DataPoint]
  implicit val hourlyReads = Json.reads[Hourly]
  implicit val responseReads = Json.reads[Response]



case class Point(
  hour: Int,
  icon: String,
  temp: Double
)

case class Response1(
  stat: String,
  data: List[Point]
)

  implicit val poitWrites = Json.writes[Point]
  implicit val response1Writess = Json.writes[Response1]


  def index() = Action.async
  {
    val url = "https://api.darksky.net/forecast/1cbbfb780a7ada23c39be9ae9871754a/45.671837,12.324886?exclude=minutely,daily,alerts,flags&units=auto";

    ws.url(url).get().map { 
	response => { 

		var jsonReponse = response.body;

		val darkSkyResponse: JsResult[Response] = Json.fromJson[Response](Json.parse(response.body))

		darkSkyResponse match {
			case r: JsSuccess[Response] => {
				var point = r.get.hourly.data(0);
			        jsonReponse = Json.toJson(new Response1("ok", r.get.hourly.data.map(point => new Point(LocalDateTime.ofEpochSecond(point.time, 0, ZoneOffset.ofHours(0)).getHour(), point.icon, point.temperature)))).toString();
			}

			case e: JsError => jsonReponse = Json.toJson(new Response1(e.toString(), List())).toString()
		}


		Ok(jsonReponse)
	}
    }
  }
}
