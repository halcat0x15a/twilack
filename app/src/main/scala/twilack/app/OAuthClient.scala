package twilack.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.ConfigFactory

import java.awt.Desktop
import java.net.URI
import java.nio.file.{Files, Path}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

import twilack.slack.SlackAPI

import twitter4j.Twitter

object OAuthClient {

  def authorize(twitter: Twitter, confPath: Path)(implicit system: ActorSystem): TwilackConfig = {
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val confPromise = Promise[TwilackConfig]
    val requestToken = twitter.getOAuthRequestToken(Twilack.serverUrl)
    val route =
      get {
        parameter("oauth_verifier") { verifier =>
          setCookie(HttpCookie("verifier", value = verifier)) {
            val uri = SlackAPI.authorize(Twilack.clientId, Twilack.scope)
            redirect(uri, StatusCodes.TemporaryRedirect)
          }
        } ~
        parameter("code") { code =>
          cookie("verifier") { verifier =>
            onSuccess(SlackAPI.access(Twilack.clientId, Twilack.clientSecret, code)) { slackResponse =>
              val slackToken = (slackResponse \ "access_token").as[String]
              val slackTeam = (slackResponse \ "team_name").as[String]
              val api = SlackAPI(slackToken)
              onComplete(api.channels.create(Twilack.channelName)) { _ =>
                val twitterToken = twitter.getOAuthAccessToken(requestToken, verifier.value)
                val conf = TwilackConfig(slackToken, twitterToken.getToken, twitterToken.getTokenSecret)
                confPromise.success(conf)
                redirect(s"https://${slackTeam}.slack.com/messages/#{Twilack.channelName}/", StatusCodes.TemporaryRedirect)
              }
            }
          }
        }
      }
    val bindingFuture = Http().bindAndHandle(route, Twilack.serverHost, Twilack.serverPort)
    Desktop.getDesktop.browse(new URI(requestToken.getAuthorizationURL))
    val conf = Await.result(confPromise.future, Duration.Inf)
    Files.write(confPath, conf.toConfig.root.render.getBytes("UTF-8"))
    bindingFuture.flatMap(_.unbind())
    conf
  }

}
