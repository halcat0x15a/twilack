package twilack.app

import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http
import com.twitter.util.Future

import com.typesafe.config.ConfigFactory

import java.awt.Desktop
import java.net.URI
import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

import twilack.slack.SlackAPI

import twitter4j._

object OAuthClient {

  def auth(twitter: Twitter, confPath: Path): TwilackConfig = {
    val promise = Promise[TwilackConfig]
    val requestToken = twitter.getOAuthRequestToken("http://localhost:8080/")
    val service = new Service[http.Request, http.Response] {
      def apply(req: http.Request): Future[http.Response] = {
        val response = Option(req.getParam("oauth_verifier")).map { verifier =>
          val res = http.Response(req.version, http.Status.TemporaryRedirect)
          res.cookies += new http.Cookie("verifier", verifier)
          res.location = SlackAPI.authorize(Twilack.clientId, "client")
          res
        }.orElse(for {
          code <- Option(req.getParam("code"))
          verifier <- req.cookies.getValue("verifier")
        } yield {
          val res = http.Response(req.version, http.Status.TemporaryRedirect)
          val slackToken = SlackAPI.access(Twilack.clientId, Twilack.clientSecret, code)
          val twitterToken = twitter.getOAuthAccessToken(requestToken, verifier)
          val conf = TwilackConfig(
            (slackToken \ "access_token").as[String],
            twitterToken.getToken,
            twitterToken.getTokenSecret
          )
          promise.success(conf)
          Files.write(confPath, conf.toConfig.root.render.getBytes("UTF-8"))
          val team = (slackToken \ "team_name").as[String]
          res.location = s"https://${team}.slack.com/"
          res
        }).getOrElse(http.Response(req.version, http.Status.BadRequest))
        Future.value(response)
      }
    }
    val server = Http.serve(":8080", service)
    Desktop.getDesktop.browse(new URI(requestToken.getAuthorizationURL))
    val conf = Await.result(promise.future, Duration.Inf)
    service.close()
    conf
  }

}
