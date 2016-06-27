package twilack.slack

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}

import org.json4s.JValue
import org.json4s.jackson.JsonMethods._

class SlackRTM(handler: JValue => Unit) extends Actor {

  def receive = {
    case Message.Text(value) => handler(parse(value))
    case Message.Close => context.stop(self)
  }

}

object SlackRTM {

  def connect(handler: JValue => Unit)(implicit factory: ActorRefFactory): ActorRef =
    factory.actorOf(Props(classOf[SlackRTM], handler))

}
