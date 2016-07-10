package twilack.slack

import akka.actor.{Actor, Props, Terminated}
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class SlackActor(api: SlackAPI) extends Actor {

  import SlackActor._

  import context.dispatcher

  var state: Option[JsValue] = None

  var failures: Int = 0

  def receive = {
    case Start =>
      start()
    case EventHandler(handler) =>
      context.actorOf(EventHandlerActor.props(handler))
    case Message(text) =>
      context.child("ws").foreach(_ ! text)
    case CurrentState =>
      state.foreach(sender ! _)
    case Session(json) =>
      state = Some(json)
      failures = 0
    case Restart =>
      val delay = failures * failures
      failures += 1
      context.system.scheduler.scheduleOnce(delay.seconds, self, Start)
    case Terminated(_) =>
      self ! Restart
  }

  def start(): Unit =
    api.rtm.start.map { json =>
      val url = (json \ "url").as[String]
      val actor = context.actorOf(WebSocketActor.props(self.path / "*"), "ws")
      context.watch(actor)
      api.httpClient.prepareGet(url).execute(WebSocketActor.upgrade(actor))
      json
    }.onComplete {
      case Success(json) =>
        self ! Session(json)
      case Failure(e) =>
        e.printStackTrace
        self ! Restart
    }

}

object SlackActor {

  def props(api: SlackAPI): Props = Props(classOf[SlackActor], api)

  case object Start

  case object Restart

  case class Message(text: String)

  case object CurrentState

  case class EventHandler(apply: Try[JsValue] => Unit)

  case class Session(state: JsValue)

}
