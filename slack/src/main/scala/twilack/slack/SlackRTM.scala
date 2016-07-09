package twilack.slack

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.actor.ActorDSL._
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait SlackRTM {

  def worker: ActorRef

  implicit val system: ActorSystem

  import SlackActor._

  import system.dispatcher

  def state(): Future[JsValue] = Future {
    implicit val i = inbox()
    worker ! GetState
    i.receive().asInstanceOf[JsValue]
  }

  def send(message: String): Unit = worker ! Sending(message)

  def start(handler: Try[JsValue] => Unit): Unit = worker ! Start

}

object SlackRTM {

  def apply(api: SlackAPI)(implicit factory: ActorSystem): SlackRTM =
    new SlackRTM {
      val system = factory
      val worker = factory.actorOf(Props(classOf[SlackActor], api))
    }

}

class SlackActor(api: SlackAPI) extends Actor {

  import SlackActor._

  import context.dispatcher

  var handler: Option[Try[JsValue] => Unit] = None

  var state: Option[JsValue] = None

  var failures: Int = 0

  def receive = {
    case Start =>
      handler.foreach(start)
    case GetState =>
      state.foreach(sender ! _)
    case SetState(json) =>
      state = Some(json)
      failures = 0
    case Sending(text) =>
      context.child("ws").foreach(_ ! text)
    case Close =>
      val delay = failures * failures
      failures += 1
      context.system.scheduler.scheduleOnce(delay.seconds, self, Start)
    case Terminated(_) => self ! Close
  }

  def start(f: Try[JsValue] => Unit): Unit =
    api.rtm.start.onComplete {
      case Success(json) =>
        val url = (json \ "url").as[String]
        val actor = context.watch(context.actorOf(Props(classOf[WebSocketActor], f), "ws"))
        api.httpClient.prepareGet(url).execute(WebSocketActor.upgrade(actor))
        self ! SetState(json)
      case Failure(e) =>
        e.printStackTrace
        self ! Close
    }

}

object SlackActor {

  case class Start(handler: Try[JsValue] => Unit)

  case object GetState

  case class SetState(state: JsValue)

  case class Received(message: String)

  case class Sending(message: String)

  case object Close

  case class Error(cause: Throwable)

}
