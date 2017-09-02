package twilack

import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, Response}
import play.api.libs.json.{Json, Writes}
import scala.concurrent.{Future, Promise}

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

  val httpClient: DefaultAsyncHttpClient = new DefaultAsyncHttpClient

  def postMessage(
    text: String,
    username: String,
    iconUrl: String,
    attachments: Seq[Attachment]
  ): Future[Response] = {
    val builder = httpClient.prepareGet("https://slack.com/api/chat.postMessage")
      .addQueryParam("token", config.slackToken)
      .addQueryParam("channel", config.slackChannel)
      .addQueryParam("text", text)
      .addQueryParam("as_user", "false")
      .addQueryParam("username", username)
      .addQueryParam("icon_url", iconUrl)
      .addQueryParam("attachments", Json.stringify(Json.toJson(attachments)))
    val result = Promise[Response]
    builder.execute(new AsyncCompletionHandler[Response] {
      override def onCompleted(response: Response) = {
        result.success(response)
        response
      }
      override def onThrowable(throwable: Throwable) = {
        result.failure(throwable)
      }
    })
    result.future
  }

}
