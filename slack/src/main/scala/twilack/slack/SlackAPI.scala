package twilack.slack

import play.api.libs.json.{Json, JsValue}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import scala.language.implicitConversions

trait SlackAPI {

  implicit def executionContext: ExecutionContext

  def accessToken: String

  private[this] def builder(method: String): RequestBuilder =
    RequestBuilder(s"https://slack.com/api/$method").addQueryParam("token", accessToken)

  private[this] def execute(builder: RequestBuilder): Future[JsValue] =
    builder.execute().map { json =>
      if ((json \ "ok").as[Boolean]) {
        json
      } else {
        throw SlackException((json \ "error").as[String])
      }
    }

  object auth {

    def test(): Future[JsValue] =
      execute(builder("auth.test"))

  }

  object channels {

    def create(name: String): Future[JsValue] =
      execute(builder("channels.create").addQueryParam("name", name))

    def info(channel: String): Future[JsValue] =
      execute(builder("channels.info").addQueryParam("channel", channel))

    def list(excludeArchived: Option[String] = None): Future[JsValue] =
      execute(builder("channels.list").addQueryParam("exclude_archived", excludeArchived))

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
      execute(builder("chat.postMessage")
        .addQueryParam("channel", channel)
        .addQueryParam("text", text)
        .addQueryParam("parse", parse)
        .addQueryParam("link_names", linkNames)
        .addQueryParam("attachments", attachments)
        .addQueryParam("unfurl_links", unfurlLinks)
        .addQueryParam("unfurl_media", unfurlMedia)
        .addQueryParam("username", username)
        .addQueryParam("as_user", asUser)
        .addQueryParam("icon_url", iconUrl)
        .addQueryParam("icon_emoji", iconEmoji))

  }

  object rtm {

    def start(): Future[JsValue] =
      execute(builder("rtm.start"))

  }

  object users {

    def identity(): Future[JsValue] =
      execute(builder("users.identity"))

  }

}

object SlackAPI {

  def apply(token: String)(implicit ec: ExecutionContext): SlackAPI =
    new SlackAPI {
      val accessToken = token
      val executionContext = ec
    }

  def authorize(clientId: String, scope: String, redirectUri: Option[String] = None, state: Option[String] = None, team: Option[String] = None): String =
    RequestBuilder("https://slack.com/oauth/authorize")
      .addQueryParam("client_id", clientId)
      .addQueryParam("scope", scope)
      .addQueryParam("redirect_uri", redirectUri)
      .addQueryParam("state", state)
      .addQueryParam("team", team)
      .build.getUrl

  def access(clientId: String, clientSecret: String, code: String, redirectUri: Option[String] = None): Future[JsValue] =
    RequestBuilder("https://slack.com/api/oauth.access")
      .addQueryParam("client_id", clientId)
      .addQueryParam("client_secret", clientSecret)
      .addQueryParam("code", code)
      .addQueryParam("redirect_uri", redirectUri)
      .execute()

}
