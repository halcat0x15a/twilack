package twilack.slack

import akka.actor.{ActorRef, ActorRefFactory}
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.json.JsValue
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait SlackRTM {

  import SlackActor._

  def worker: ActorRef

  implicit def dispatcher: ExecutionContext

  implicit val timeout: Timeout = Timeout(1.minute)

  def state(): Future[JsValue] = (worker ? CurrentState).map(_.asInstanceOf[JsValue])

  def send(message: String): Unit = worker ! Message(message)

  def onEvent(handler: JsValue => Unit): Unit =
    worker ! EventHandler {
      case Success(json) => handler(json)
      case Failure(_) =>
    }

  def onError(handler: Throwable => Unit): Unit =
    worker ! EventHandler {
      case Success(_) =>
      case Failure(e) => handler(e)
    }

  def start(): Unit = worker ! Start

}

object SlackRTM {

  def apply(api: SlackAPI)(implicit factory: ActorRefFactory): SlackRTM =
    new SlackRTM {
      val worker = factory.actorOf(SlackActor.props(api))
      val dispatcher = factory.dispatcher
    }

}
