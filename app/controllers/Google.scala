package Google

import play.api.libs.json._

case class GoogleAddress(
  long_name: String,
  short_name: String,
  types: List[String]
)
object GoogleAddress
{
  implicit val reads = Json.reads[GoogleAddress]
}

case class GoogleCoordinates(
  lat: Double,
  lng: Double
)
object GoogleCoordinates
{
  implicit val reads = Json.reads[GoogleCoordinates]
}

case class GoogleBounds(
  northeast: GoogleCoordinates,
  southwest: GoogleCoordinates
)
object GoogleBounds
{
  implicit val reads = Json.reads[GoogleBounds]
}

case class GoogleGeometry(
  bounds: GoogleBounds,
  location: GoogleCoordinates,
  location_type: String,
  viewport: GoogleBounds
)
object GoogleGeometry
{
  implicit val reads = Json.reads[GoogleGeometry]
}

case class GoogleResult(
  address_components: List[GoogleAddress],
  formatted_address: String,
  geometry: GoogleGeometry,
  place_id: String,
  types: List[String]
)
object GoogleResult
{
  implicit val reads = Json.reads[GoogleResult]
}

case class GoogleResponse(
  results: List[GoogleResult],
  status: String
)
object GoogleResponse
{
  implicit val reads = Json.reads[GoogleResponse]
}
