package twilack

import org.asynchttpclient.{AsyncCompletionHandler, BoundRequestBuilder, DefaultAsyncHttpClient, Response}
import play.api.libs.json.{Json, JsValue, Writes}
import scala.concurrent.{Future, Promise}
import scala.util.Try

case class Attachment(
  author_name: Option[String] = None,
  author_link: Option[String] = None,
  author_icon: Option[String] = None,
  color: Option[String] = None,
  fallback: Option[String] = None,
  footer: Option[String] = None,
  footer_icon: Option[String] = None,
  image_url: Option[String] = None,
  text: Option[String] = None,
  ts: Option[Long] = None
)

object Attachment {
  implicit val writes: Writes[Attachment] = Json.writes[Attachment]
}

class SlackClient(config: TwilackConfig) {

  def postMessage(
    text: String,
    username: String,
    iconUrl: String,
    attachments: Seq[Attachment]
  ): Future[JsValue] =
    SlackClient.execute("chat.postMessage")(_.addQueryParam("token", config.slackToken)
      .addQueryParam("channel", config.slackChannel)
      .addQueryParam("text", text)
      .addQueryParam("as_user", "false")
      .addQueryParam("username", username)
      .addQueryParam("icon_url", iconUrl)
      .addQueryParam("attachments", Json.stringify(Json.toJson(attachments))))

  def createChannel: Future[JsValue] =
    SlackClient.execute("channels.create")(_.addQueryParam("token", config.slackToken)
      .addQueryParam("name", config.slackChannel))

}

object SlackClient {

  val httpClient: DefaultAsyncHttpClient = new DefaultAsyncHttpClient

  def execute(method: String)(f: BoundRequestBuilder => BoundRequestBuilder): Future[JsValue] = {
    val result = Promise[JsValue]
    f(httpClient.prepareGet(s"https://slack.com/api/$method")).execute(new AsyncCompletionHandler[Response] {
      override def onCompleted(response: Response) = {
        result.complete(Try(Json.parse(response.getResponseBody)))
        response
      }
      override def onThrowable(throwable: Throwable) = {
        result.failure(throwable)
      }
    })
    result.future
  }

  def authorize: String =
    httpClient.prepareGet("https://slack.com/oauth/authorize")
      .addQueryParam("client_id", Twilack.clientId)
      .addQueryParam("scope", Twilack.scope)
      .build
      .getUrl

  def access(code: String): Future[JsValue] =
    execute("oauth.access")(_.addQueryParam("client_id", Twilack.clientId)
      .addQueryParam("client_secret", Twilack.clientSecret)
      .addQueryParam("code", code))

}
