package twilack.slack

import akka.actor.{Actor, ActorPath, ActorRef, Props}
import org.asynchttpclient.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}
import play.api.libs.json.{Json, JsValue}
import scala.util.{Failure, Success, Try}

class WebSocketActor(path: ActorPath) extends Actor {

  import WebSocketActor._
  import EventHandlerActor._

  var websocket: Option[WebSocket] = None

  def receive = {
    case Received(text) =>
      context.actorSelection(path) ! Event(Success(Json.parse(text)))
    case Sending(text) =>
      websocket.foreach(_.sendMessage(text))
    case Open(ws) =>
      websocket = Some(ws)
    case Close =>
      context.stop(self)
    case Error(e) =>
      context.actorSelection(path) ! Event(Failure(e))
  }

}

object WebSocketActor {

  def props(path: ActorPath): Props = Props(classOf[WebSocketActor], path)

  def upgrade(actor: ActorRef): WebSocketUpgradeHandler =
    new WebSocketUpgradeHandler.Builder()
      .addWebSocketListener(new WebSocketTextListener {
        def onMessage(message: String): Unit = actor ! Received(message)
        def onOpen(websocket: WebSocket): Unit = actor ! Open(websocket)
        def onClose(websocket: WebSocket): Unit = actor ! Close
        def onError(throwable: Throwable): Unit = actor ! Error(throwable)
      })
      .build

  case class Received(message: String)

  case class Sending(message: String)

  case class Open(websocket: WebSocket)

  case object Close

  case class Error(cause: Throwable)

}
