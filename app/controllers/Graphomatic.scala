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


case class Graphomatic2Response(
  s: String,
  l: String,
  h: List[Long],
  i: List[Int],
  t: List[Int]
)
object Graphomatic2Response
{
  implicit val writes = Json.writes[Graphomatic2Response]
}