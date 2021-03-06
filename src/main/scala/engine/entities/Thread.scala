package engine.entities

import engine.entities.FileImplicits._
import engine.entities.MarkupImplicits._
import spray.json.DefaultJsonProtocol._
import spray.json._

case class Thread
(
  id: BigInt,
  URL: String,
  subject: String,
  content: String,
  postsCount: Int,
  timestamp: Int,
  files: List[File],
  decorations: List[DecorationMarkup],
  links: List[LinkMarkup],
  replies: List[ReplyMarkup]
)

object ThreadImplicits {
  implicit val threadFormat: RootJsonFormat[Thread] = jsonFormat10(Thread)
}

