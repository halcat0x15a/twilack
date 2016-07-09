package twilack.slack

import org.asynchttpclient.{AsyncCompletionHandler, BoundRequestBuilder, DefaultAsyncHttpClient, RequestBuilder, Response}

import play.api.libs.json.{Json, JsValue}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import scala.language.implicitConversions

trait SlackAPI {

  implicit def executionContext: ExecutionContext

  def accessToken: String

  val httpClient = new DefaultAsyncHttpClient

  def execute(method: String, params: (String, SlackAPI.Param)*): Future[JsValue] = {
    val request = httpClient
      .prepareGet(s"https://slack.com/api/$method")
      .addQueryParam("token", accessToken)
    params.foreach {
      case (key, param) => param.value.foreach(request.addQueryParam(key, _))
    }
    SlackAPI.execute(request) { response =>
      Try {
        val json = Json.parse(response.getResponseBody)
        if ((json \ "ok").as[Boolean]) {
          json
        } else {
          throw SlackException((json \ "error").as[String])
        }
      }
    }
  }

  object auth {

    def test(): Future[JsValue] =
      execute("auth.test")

  }

  object channels {

    def create(name: String): Future[JsValue] =
      execute("channels.create", "name" -> name)

    def info(channel: String): Future[JsValue] =
      execute("channels.info", "channel" -> channel)

    def list(excludeArchived: Option[String] = None): Future[JsValue] =
      execute("channels.list", "exclude_archived" -> excludeArchived)

  }

  object chat {

    def postMessage(
      channel: String,
      text: String,
      parse: Option[String] = None,
      linkNames: Option[String] = None,
      attachments: Option[List[Attachment]] = None,
      unfurlLinks: Option[Boolean] = None,
      unfurlMedia: Option[Boolean] = None,
      username: Option[String] = None,
      asUser: Option[Boolean] = None,
      iconUrl: Option[String] = None,
      iconEmoji: Option[String] = None
    ): Future[JsValue] =
      execute(
        "chat.postMessage",
        "channel" -> channel,
        "text" -> text,
        "parse" -> parse,
        "link_names" -> linkNames,
        "attachments" -> attachments,
        "unfurl_links" -> unfurlLinks,
        "unfurl_media" -> unfurlMedia,
        "username" -> username,
        "as_user" -> asUser,
        "icon_url" -> iconUrl,
        "icon_emoji" -> iconEmoji
      )

  }

  object rtm {

    def start(): Future[JsValue] =
      execute("rtm.start")

  }

  object users {

    def identity(): Future[JsValue] =
      execute("users.identity")

  }

}

object SlackAPI {

  def authorize(clientId: String, scope: String, redirectUri: Option[String] = None, state: Option[String] = None, team: Option[String] = None): String = {
    val builder = new RequestBuilder()
      .setUrl("https://slack.com/oauth/authorize")
      .addQueryParam("client_id", clientId)
      .addQueryParam("scope", scope)
    redirectUri.foreach(builder.addQueryParam("redirect_uri", _))
    state.foreach(builder.addQueryParam("state", _))
    team.foreach(builder.addQueryParam("team", _))
    builder.build.getUrl
  }

  def apply(token: String)(implicit ec: ExecutionContext): SlackAPI =
    new SlackAPI {
      val accessToken = token
      val executionContext = ec
    }

  def execute[A](request: BoundRequestBuilder)(onComplete: Response => Try[A]): Future[A] = {
    val result = Promise[A]
    request.execute(new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response) = {
        result.complete(onComplete(response))
        response
      }
      override def onThrowable(throwable: Throwable) = {
        result.failure(throwable)
      }
    })
    result.future
  }

  def access(clientId: String, clientSecret: String, code: String, redirectUri: Option[String] = None): Future[JsValue] = {
    val request = new DefaultAsyncHttpClient()
      .prepareGet("https://slack.com/api/oauth.access")
      .addQueryParam("client_id", clientId)
      .addQueryParam("client_secret", clientSecret)
      .addQueryParam("code", code)
    redirectUri.foreach(request.addQueryParam("redirect_uri", _))
    execute(request) { response =>
      Try(Json.parse(response.getResponseBody))
    }
  }

  case class Param(value: Option[String])

  object Param {

    implicit def booleanToParam(boolean: Boolean): Param = Param(Some(boolean.toString))

    implicit def stringToParam(string: String): Param = Param(Some(string))

    implicit def optionToParam[A](option: Option[A])(implicit ev: A => Param): Param =
      Param(option.flatMap(a => ev(a).value))

    implicit def attachmentsToParam(attachments: List[Attachment]): Param =
      Param(Some(Json.stringify(Json.toJson(attachments))))

  }

}
