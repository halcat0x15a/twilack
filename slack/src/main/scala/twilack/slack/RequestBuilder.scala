package twilack.slack

import org.asynchttpclient.{AsyncCompletionHandler, BoundRequestBuilder, Request, Response}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

case class RequestBuilder(builder: BoundRequestBuilder) extends AnyVal {

  def addQueryParam[A](key: String, value: A)(implicit A: QueryParam[A]): RequestBuilder =
    RequestBuilder(A(value).fold(builder)(builder.addQueryParam(key, _)))

  def build: Request = builder.build

  def execute(): Future[JsValue] = {
    val result = Promise[JsValue]
    builder.execute(new AsyncCompletionHandler[Response] {
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

}

object RequestBuilder {

  def apply(url: String): RequestBuilder = RequestBuilder(httpClient.prepareGet(url))

}
