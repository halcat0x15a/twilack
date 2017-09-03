package twilack

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import java.awt.Desktop
import java.net.URI
import java.nio.file.{Files, Path}
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import twitter4j.auth.OAuthSupport

object OAuthClient {

  def authorize(twitter: OAuthSupport, configPath: Path): TwilackConfig = {
    val channel = sys.props.get("twilack.channel").getOrElse(Twilack.channelName)
    implicit val system = ActorSystem("twilack")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val configPromise = Promise[TwilackConfig]
    val requestToken = twitter.getOAuthRequestToken(s"http://${Twilack.host}:${Twilack.port}/")
    val route =
      get {
        parameter("oauth_verifier") { verifier =>
          setCookie(HttpCookie("verifier", value = verifier)) {
            redirect(SlackClient.authorize, StatusCodes.TemporaryRedirect)
          }
        } ~
        parameter("code") { code =>
          cookie("verifier") { verifier =>
            onSuccess(SlackClient.access(code)) { res =>
              val slackToken = (res \ "access_token").as[String]
              val team = (res \ "team_name").as[String]
              val twitterToken = twitter.getOAuthAccessToken(requestToken, verifier.value)
              val config = TwilackConfig(slackToken, channel, twitterToken.getToken, twitterToken.getTokenSecret)
              onSuccess(new SlackClient(config).createChannel) { _ =>
                configPromise.success(config)
                redirect(s"https://${team}.slack.com/messages/${config.slackChannel}/", StatusCodes.TemporaryRedirect)
              }
            }
          }
        }
      }
    val bindingFuture = Http().bindAndHandle(route, Twilack.host, Twilack.port)
    Desktop.getDesktop.browse(new URI(requestToken.getAuthorizationURL))
    val config = Await.result(configPromise.future, Duration.Inf)
    Files.write(configPath, config.render.getBytes("UTF-8"))
    bindingFuture.flatMap(_.unbind())
    config
  }

}
