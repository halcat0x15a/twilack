package twilack.slack

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import org.asynchttpclient.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class SlackActor(api: SlackAPI) extends Actor {

  import SlackActor._

  import context.dispatcher

  var failures: Int = 0

  var state: Option[JsValue] = None

  var websocket: Option[WebSocket] = None

  def receive = {
    case Start =>
      if (state.isEmpty) {
        start()
      }
    case CurrentState =>
      state.foreach(sender ! _)
    case Session(json) =>
      state = Some(json)
      failures = 0
    case Restart =>
      state = None
      val delay = failures * failures
      failures += 1
      context.system.scheduler.scheduleOnce(delay.seconds, self, Start)
    case Sending(message) =>
      websocket.foreach(_.sendMessage(message))
    case Received(message) =>
      context.children.foreach(_ ! Event(Success(Json.parse(message))))
    case Error(e) =>
      context.children.foreach(_ ! Event(Failure(e)))
    case Open(ws) =>
      websocket = Some(ws)
    case Close =>
      websocket = None
      self ! Restart
    case props: Props =>
      context.actorOf(props)
  }

  def start(): Unit =
    api.rtm.start.map { json =>
      val url = (json \ "url").as[String]
      val handler = new WebSocketUpgradeHandler.Builder()
        .addWebSocketListener(new WebSocketTextListener {
          def onMessage(message: String): Unit = self ! Received(message)
          def onOpen(websocket: WebSocket): Unit = self ! Open(websocket)
          def onClose(websocket: WebSocket): Unit = self ! Close
          def onError(throwable: Throwable): Unit = self ! Error(throwable)
        })
        .build
        httpClient.prepareGet(url).execute(handler)
      json
    }.onComplete {
      case Success(json) =>
        self ! Session(json)
      case Failure(e) =>
        self ! Error(e)
        self ! Restart
    }

}

object SlackActor {

  def apply(api: SlackAPI)(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(classOf[SlackActor], api))

  case object Start

  case object CurrentState

  case class Session(state: JsValue)

  case object Restart

  case class Sending(message: String)

  case class Received(message: String)

  case class Error(cause: Throwable)

  case class Open(websocket: WebSocket)

  case object Close

  case class Event(result: Try[JsValue])

  class EventHandler(handler: Try[JsValue] => Unit) extends Actor {
    def receive = {
      case Event(result) => handler(result)
    }
  }

  object EventHandler {
    def apply(handler: Try[JsValue] => Unit): Props = Props(classOf[EventHandler], handler)
  }

}
