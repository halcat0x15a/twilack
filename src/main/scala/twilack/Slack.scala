package twilack

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

import slack.api.SlackApiClient
import slack.models._

import twitter4j.{Twitter, TwitterException}

class SlackEventHandler(twitter: Twitter, slack: SlackApiClient, id: String, name: String, home: String)(implicit ec: ExecutionContext) extends (SlackEvent => Unit) {

  def getStatusId(json: JsValue): Option[Long] =
    if ((json \ "type").as[String] == "message")
      Some(((json \ "message" \ "attachments")(0) \ "fallback").as[String].toLong)
    else
      None

  def onMessage(message: Message): Unit =
    if (message.user == id) {
      val text = message.text
        .replace(s"<@$id>", s"@$name")
        .replaceAll("<[@#!]\\w+>", "")
      try {
        twitter.updateStatus(text)
      } catch {
        case e: TwitterException => slack.postChatMessage(e.getMessage, home)
      }
    }

  def apply(event: SlackEvent): Unit =
    event match {
      case message: Message => onMessage(message)
      case star: StarAdded => getStatusId(star.item).foreach(twitter.createFavorite)
      case star: StarRemoved => getStatusId(star.item).foreach(twitter.destroyFavorite)
      case _ => println(event)
    }

}
