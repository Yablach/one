package engine.imageboards.abstractimageboard

import engine.entities.BoardImplicits._
import engine.entities.PostImplicits._
import engine.entities.ThreadImplicits._
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import spray.json._


object AbstractImageBoardImplicits extends DefaultJsonProtocol with NullOptions {

  implicit object AbstractImageBoardFormat extends RootJsonFormat[AbstractImageBoard] {
    override def write(imageBoard: AbstractImageBoard): JsValue = {
      JsObject(
        "id" -> JsNumber(imageBoard.id),
        "name" -> JsString(imageBoard.name),
        "baseURL" -> JsString(imageBoard.baseURL),
        "captcha" -> imageBoard.captcha.toJson,
        "maxImages" -> JsNumber(imageBoard.maxImages),
        "logo" -> JsString(imageBoard.logo),
        "label" -> JsString(imageBoard.label),
        "highlight" -> JsString(imageBoard.highlight),
        "boards" -> imageBoard.boards.toJson
      )
    }

    override def read(json: JsValue): AbstractImageBoard = ???
  }

  implicit val formatPostResponseFormat: RootJsonFormat[FormatPostResponse] =
    jsonFormat4(FormatPostResponse)
  implicit val fetchPostsResponseFormat: RootJsonFormat[FetchPostsResponse] =
    jsonFormat2(FetchPostsResponse)
  implicit val captchaFormat: RootJsonFormat[Captcha] =
    jsonFormat3(Captcha)
  implicit val formatPostRequestFormat: RootJsonFormat[FormatPostRequest] =
    jsonFormat6(FormatPostRequest)
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] =
    jsonFormat1(ErrorResponse)
}