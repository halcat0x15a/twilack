package twilack.slack

import akka.actor.{ActorRef, ActorRefFactory, Props}

import org.asynchttpclient.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}

import play.api.libs.json.JsValue

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait SlackRTM {

  def state: JsValue

  def worker: ActorRef

  def send(message: String): Unit = worker ! SlackRTMActor.Sending(message)

  def onComplete(f: Try[JsValue] => Unit): Unit = worker ! SlackRTMActor.Handler(Some(f))

}

object SlackRTM {

  def start(api: SlackAPI)(implicit factory: ActorRefFactory, ec: ExecutionContext): Future[SlackRTM] =
    api.rtm.start.map { json =>
      val actorRef = factory.actorOf(Props[SlackRTMActor])
      val url = (json \ "url").as[String]
      val handler = new WebSocketUpgradeHandler.Builder()
        .addWebSocketListener(new WebSocketTextListener {
          def onMessage(message: String): Unit = actorRef ! SlackRTMActor.Received(message)
          def onOpen(websocket: WebSocket): Unit = actorRef ! SlackRTMActor.Open(websocket)
          def onClose(websocket: WebSocket): Unit = actorRef ! SlackRTMActor.Close(websocket)
          def onError(throwable: Throwable): Unit = actorRef ! SlackRTMActor.Failure(throwable)
        })
        .build
      api.httpClient.prepareGet(url).execute(handler)
      new SlackRTM {
        val state = json
        val worker = actorRef
      }
    }

}
