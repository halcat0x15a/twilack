package twilack

import org.asynchttpclient.{AsyncCompletionHandler, DefaultAsyncHttpClient, Response}
import scala.concurrent.{Future, Promise}

class SlackClient(config: TwilackConfig) {

  val httpClient: DefaultAsyncHttpClient = new DefaultAsyncHttpClient

  def postMessage(
    text: String,
    username: String,
    iconUrl: String
  ): Future[Response] = {
    val builder = httpClient.prepareGet("https://slack.com/api/chat.postMessage")
      .addQueryParam("token", config.slackToken)
      .addQueryParam("channel", config.slackChannel)
      .addQueryParam("text", text)
      .addQueryParam("as_user", "false")
      .addQueryParam("username", username)
      .addQueryParam("icon_url", iconUrl)
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
