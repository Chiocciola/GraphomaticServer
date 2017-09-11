package DarkSky

import play.api.libs.json._

case class DataPoint(
  time: Int,
  summary: String,
  icon: String,
  precipIntensity: Double,
  precipProbability: Double,
  precipType: Option[String],
  temperature: Double,
  apparentTemperature: Double,
  dewPoint: Double,
  humidity: Double,
  pressure: Double,
  windSpeed: Double,
  windGust: Double,
  windBearing: Double,
  cloudCover: Double,
  uvIndex: Int,
  visibility: Option[Double],
  ozone: Double
)
object DataPoint
{
  implicit val reads = Json.reads[DataPoint]
}

case class Hourly(
  summary: String,
  icon: String,
  data: List[DataPoint]
)
object Hourly
{
  implicit val reads = Json.reads[Hourly]
}

case class DarkSkyResponse(
  latitude: Double,
  longitude: Double,
  timezone: String,
  currently: DataPoint,
  hourly: Hourly,
  offset: Double
)
object DarkSkyResponse
{
  implicit val reads = Json.reads[DarkSkyResponse]
}
