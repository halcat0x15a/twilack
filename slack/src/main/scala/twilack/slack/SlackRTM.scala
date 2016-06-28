package twilack.slack

import akka.actor.{ActorRef, ActorRefFactory, Props}

import org.asynchttpclient.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

trait SlackRTM {

  def worker: ActorRef

  def send(message: String): Unit = worker ! SlackRTMActor.Sending(message)

}

object SlackRTM {

  def apply(api: SlackAPI)(handler: JsValue => Unit)(implicit factory: ActorRefFactory, ec: ExecutionContext): SlackRTM = {
    val actorRef = factory.actorOf(Props(classOf[SlackRTMActor], handler))
    api.startRTM.onSuccess {
      case json =>
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
    }
    new SlackRTM {
      val worker = actorRef
    }
  }

}
