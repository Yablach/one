package engine.imageboards.fourchan

import akka.http.scaladsl.model.headers.HttpCookiePair
import client.Client
import engine.entities.{Board, File, LinkMarkup, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.fourchan.FourChanImplicits._
import engine.imageboards.fourchan.FourChanStructs.{FourChanBoardsResponse, FourChanFormatPostData, FourChanPostsResponse, FourChanThreadsResponse}
import engine.utils.{Extracted, Extractor}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

class FourChan(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 1
  override val name: String = "4chan"
  override val baseURL: String = "https://a.4cdn.org"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://boards.4chan.org",
      kind = "reCAPTCHA v2",
      key = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc"
    )
  )
  override val maxImages: Int = 1
  override val logo: String = "https://channy.io/4ch-icon.png"
  override val label: String = "supported by /r/4chan subreddit"
  override val highlight: String = "#117743"

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  val spans = List(
    ("deadlink", "strikethrough"),
    ("quote", "quote")
  )

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = {
    Extractor(
      text,
      body => {
        val spannedElements = spans
          .flatMap(
            x =>
              body
                .getElementsByTag("span")
                .asScala
                .toList
                .flatMap(
                  element =>
                    element
                      .getElementsByClass(x._1)
                      .asScala
                      .toList
                      .map(element => (element, x._2))
                )
          )
        spannedElements
      }, body => {
        val elements = body.getElementsByTag("a")
        val bodyText = body.wholeText()
        elements
          .iterator()
          .asScala
          .flatMap(
            element => {
              val elementText = element.wholeText()
              val indexes = Extractor.indexesOf(bodyText, elementText)
              indexes
                .map(
                  index =>
                    LinkMarkup(
                      start = index,
                      end = index + elementText.length,
                      kind = "external",
                      content = element.attr("href"),
                      link = element.attr("href")
                    )
                )
            }
          ).toList
      }, body => {
        val elements = body.getElementsByClass("quotelink")
        val bodyText = body.wholeText()
        elements
          .iterator()
          .asScala
          .flatMap(
            e => {
              val elementText = e.wholeText()
              val indexes = Extractor.indexesOf(bodyText, elementText)
              indexes
                .map(
                  index =>
                    ReplyMarkup(
                      start = index,
                      end = index + elementText.length,
                      kind = "reply",
                      thread = 0,
                      post = Try(BigInt(e.attr("href").drop(2))).getOrElse(0)
                    )
                )
            }
          ).toList
      }
    )
  }

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .getJSON(s"${this.baseURL}/boards.json")
      .map(
        _
          .asJsObject
          .getFields("boards")
          .head
          .convertTo[List[FourChanBoardsResponse]]
          .map(
            board =>
              Board(
                id = board.board,
                name = board.title
              )
          )
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          List.empty
      }
  }

  override def fetchThreads(board: String)
                           (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, List[Thread]]] = {
    this
      .client
      .getJSON(s"${this.baseURL}/$board/catalog.json")
      .map(
        response =>
          Right(
            response
              .convertTo[JsArray]
              .elements
              .toList
              .flatMap(
                page => {
                  page
                    .asJsObject
                    .getFields("threads")
                    .head
                    .convertTo[List[FourChanThreadsResponse]]
                }
              )
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.com.getOrElse(""))
                  Thread(
                    id = thread.no,
                    URL = s"https://boards.4chan.org/$board/thread/${thread.no}",
                    subject = extracted.content,
                    content = extracted.content,
                    postsCount = thread.replies + 1,
                    timestamp = thread.time,
                    files = thread.filename
                      .map(
                        filename =>
                          List(
                            File(
                              name = filename,
                              full = s"https://i.4cdn.org/$board/${thread.tim.get}${thread.ext.get}",
                              thumbnail = s"https://i.4cdn.org/$board/${thread.tim.get}s.jpg"
                            )
                          )
                      ).getOrElse(List.empty),
                    decorations = extracted.decorations,
                    links = extracted.links,
                    replies = extracted.replies,
                  )
                }
              )
          )
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Board unavailable"))
      }
  }

  override def fetchPosts(board: String, thread: Int, since: Int)
                         (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this
      .client
      .getJSON(s"${this.baseURL}/$board/thread/$thread.json")
      .map(
        response =>
          response
            .asJsObject
            .getFields("posts")
            .head
            .convertTo[List[FourChanPostsResponse]]
      )
      .map(
        posts => {
          val formattedPosts = posts
            .map(
              post => {
                val extracted = this.fetchMarkups(post.com.getOrElse(""))
                Post(
                  id = post.no,
                  content = extracted.content,
                  timestamp = post.time,
                  files = post.filename
                    .map(
                      filename =>
                        List(
                          File(
                            name = filename,
                            full = s"https://i.4cdn.org/$board/${post.tim.get}${post.ext.get}",
                            thumbnail = s"https://i.4cdn.org/$board/${post.tim.get}s.jpg"
                          )
                        )
                    ).getOrElse(List.empty),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies.map(
                    reply =>
                      ReplyMarkup(
                        start = reply.start,
                        end = reply.end,
                        kind = reply.kind,
                        thread = post.resto,
                        post = reply.post
                      )
                  ),
                  selfReplies = List.empty
                )
              }
            )
          val originalPost: Post = formattedPosts.head
          Right(
            FetchPostsResponse(
              thread = Thread(
                id = originalPost.id,
                URL = s"https://boards.4chan.org/$board/thread/$thread",
                subject = originalPost.content,
                content = originalPost.content,
                postsCount = formattedPosts.length + 1,
                timestamp = originalPost.timestamp,
                files = originalPost.files,
                decorations = originalPost.decorations,
                links = originalPost.links,
                replies = originalPost.replies.map(
                  reply =>
                    ReplyMarkup(
                      start = reply.start,
                      end = reply.end,
                      kind = reply.kind,
                      thread = originalPost.id,
                      post = reply.post
                    )
                ),
              ),
              posts = formattedPosts
                .map(
                  post =>
                    Post(
                      id = post.id,
                      content = post.content,
                      timestamp = post.timestamp,
                      files = post.files,
                      decorations = post.decorations,
                      links = post.links,
                      replies = post.replies,
                      selfReplies = this.fetchSelfReplies(post.id, formattedPosts)
                    )
                )
                .drop(since)
            )
          )
        }
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Thread unavailable"))
      }
  }


  override def formatPost(post: FormatPostRequest): FormatPostResponse = {
    FormatPostResponse(
      url = s"https://sys.4chan.org/${post.board}/post",
      headers = Map(
        "Referer" -> "https://boards.4chan.org/"
      ),
      images = List("upfile"),
      data = FourChanFormatPostData(
        resto = post.thread.orNull,
        com = post.text,
        `g-recaptcha-response` = post.captcha
      ).toJson
    )
  }
}