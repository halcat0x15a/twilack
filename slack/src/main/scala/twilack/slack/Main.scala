package twilack.slack

import akka.actor.ActorSystem

object Main extends App {
  implicit val system = ActorSystem("slack")
  val rtm = SlackRTM.connect(json => println(json))
}
