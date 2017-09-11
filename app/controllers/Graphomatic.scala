package Graphomatic

import play.api.libs.json._

case class GraphomaticPoint(
  h: Long,
  i: String,
  t: Int
)
object GraphomaticPoint
{
  implicit val writes = Json.writes[GraphomaticPoint]
}

case class GraphomaticResponse(
  stat: String,
  location: String,
  hourly: List[GraphomaticPoint]
)
object GraphomaticResponse
{
  implicit val writes = Json.writes[GraphomaticResponse]
}
