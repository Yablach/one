package client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Client(
              implicit actorSystem: ActorSystem,
              materializer: ActorMaterializer,
            ) {

  private val innerClient: HttpExt = Http()
  this.innerClient.setDefaultClientHttpsContext(
    this.innerClient.createClientHttpsContext(
      AkkaSSLConfig().mapSettings(
        settings => settings
          .withLoose(
            settings
              .loose
              .withAllowWeakProtocols(true)
              .withAllowWeakCiphers(true)
              .withAllowUnsafeRenegotiation(Some(true))
              .withAcceptAnyCertificate(true)
            // .withDisableHostnameVerification(true)
            // .withDisableSNI(true)
          )
      )
    )
  )


  def GET(url: String)(implicit cookies: List[HttpCookiePair] = List.empty): Future[JsValue] = {
    val headers = if (cookies.isEmpty) List.empty else List(Cookie(cookies))

    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url,
      headers = headers
    )
    println(request.cookies)

    val response = this.innerClient.singleRequest(request = request)
    response.flatMap(r => Unmarshal(r).to[String].map(_.parseJson))
  }
}
