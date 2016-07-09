package twilack.slack

import akka.actor.{Actor, ActorRef}
import org.asynchttpclient.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}
import play.api.libs.json.{Json, JsValue}
import scala.util.{Failure, Success, Try}

class WebSocketActor(onComplete: Try[JsValue] => Unit) extends Actor {

  import WebSocketActor._

  var websocket: Option[WebSocket] = None

  def receive = {
    case Received(text) =>
      onComplete(Success(Json.parse(text)))
    case Sending(text) =>
      websocket.foreach(_.sendMessage(text))
    case Open(ws) =>
      websocket = Some(ws)
    case Close =>
      context.stop(self)
    case Error(e) =>
      onComplete(Failure(e))
  }

}

object WebSocketActor {

  def upgrade(actorRef: ActorRef): WebSocketUpgradeHandler =
    new WebSocketUpgradeHandler.Builder()
      .addWebSocketListener(new WebSocketTextListener {
        def onMessage(message: String): Unit = actorRef ! Received(message)
        def onOpen(websocket: WebSocket): Unit = actorRef ! Open(websocket)
        def onClose(websocket: WebSocket): Unit = actorRef ! Close
        def onError(throwable: Throwable): Unit = actorRef ! Error(throwable)
      })
      .build

  case class Received(message: String)

  case class Sending(message: String)

  case class Open(websocket: WebSocket)

  case object Close

  case class Error(cause: Throwable)

}
