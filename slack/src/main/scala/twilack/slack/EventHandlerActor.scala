package twilack.slack

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.JsValue
import scala.util.Try

class EventHandlerActor(handler: Try[JsValue] => Unit) extends Actor {

  import EventHandlerActor._

  def receive = {
    case Event(result) => handler(result)
  }

}

object EventHandlerActor {

  def props(handler: Try[JsValue] => Unit): Props = Props(classOf[EventHandlerActor], handler)

  case class Event(result: Try[JsValue])

}
