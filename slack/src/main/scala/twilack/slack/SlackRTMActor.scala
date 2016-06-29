package twilack.slack

import akka.actor.Actor

import org.asynchttpclient.ws.WebSocket

import play.api.libs.json.{Json, JsValue}

import scala.util.{Failure, Success, Try}

class SlackRTMActor extends Actor {

  var handler: Option[Try[JsValue] => Unit] = None

  var websocket: Option[WebSocket] = None

  def receive = {
    case SlackRTMActor.Received(message) =>
      handler.foreach(_(Success(Json.parse(message))))
    case SlackRTMActor.Sending(message) =>
      websocket.foreach(_.sendMessage(message))
    case SlackRTMActor.Open(ws) =>
      websocket = Some(ws)
    case SlackRTMActor.Close(_) =>
      context.stop(self)
    case SlackRTMActor.Failure(throwable) =>
      handler.foreach(_(Failure(throwable)))
    case SlackRTMActor.Handler(onComplete) =>
      handler = onComplete
  }

}

object SlackRTMActor {

  sealed abstract class Message

  case class Received(message: String) extends Message

  case class Sending(message: String) extends Message

  case class Open(websocket: WebSocket) extends Message

  case class Close(websocket: WebSocket) extends Message

  case class Failure(cause: Throwable) extends Message

  case class Handler(onComplete: Option[Try[JsValue] => Unit]) extends Message

}
