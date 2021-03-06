package engine.imageboards.abstractimageboard

import engine.entities.{Post, Thread}
import spray.json.JsValue

object AbstractImageBoardStructs {

  case class FetchPostsResponse
  (
    thread: Thread,
    posts: List[Post]
  )

  case class Captcha
  (
    url: String,
    kind: String,
    key: String
  )

  case class FormatPostRequest
  (
    board: String,
    thread: Option[String],
    subject: String,
    text: String,
    images: Int,
    captcha: Option[String]
  )

  case class FormatPostResponse
  (
    url: String,
    headers: Map[String, String] = Map.empty,
    images: List[String],
    data: JsValue
  )

  case class ErrorResponse
  (
    error: String
  )

}