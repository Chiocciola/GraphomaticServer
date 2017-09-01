package Google

import play.api.libs.json._


case class GoogleAddress(
  long_name: String,
  short_name: String,
  types: List[String]
)
case class GoogleCoordinates(
  lat: Double,
  lng: Double
)
case class GoogleBounds(
  northeast: GoogleCoordinates,
  southwest: GoogleCoordinates
)
case class GoogleGeometry(
  bounds: GoogleBounds,
  location: GoogleCoordinates,
  location_type: String,
  viewport: GoogleBounds
)
case class GoogleResult(
  address_components: List[GoogleAddress],
  formatted_address: String,
  geometry: GoogleGeometry,
  place_id: String,
  types: List[String]
)
case class GoogleResponse(
  results: List[GoogleResult],
  status: String
)