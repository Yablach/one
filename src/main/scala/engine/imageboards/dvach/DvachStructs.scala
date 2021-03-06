package engine.imageboards.dvach

object DvachStructs {

  case class DvachBoardsResponse
  (
    id: String,
    name: String
  )

  case class DvachFileResponse
  (
    fullname: Option[String],
    displayname: Option[String],
    path: String,
    thumbnail: String
  )

  case class DvachThreadsResponse
  (
    num: String,
    subject: String,
    comment: String,
    `posts_count`: Int,
    timestamp: Int,
    files: Option[List[DvachFileResponse]]
  )

  case class DvachPostsResponse
  (
    num: String,
    subject: Option[String],
    comment: String,
    timestamp: Int,
    files: Option[List[DvachFileResponse]]
  )

  case class DvachFormatPostData
  (
    task: String = "post",
    board: String,
    thread: String,
    subject: String,
    comment: String,
    `captcha_type`: String = "invisible_recaptcha",
    `captcha-key`: String,
    `g-recaptcha-response`: Option[String]
  )

}