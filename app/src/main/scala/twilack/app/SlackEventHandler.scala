package twilack.app

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

import twilack.slack.SlackAPI

import twitter4j.{Twitter, TwitterException}

class SlackEventHandler(twitter: Twitter, slack: SlackAPI, user: TwilackUser)(implicit ec: ExecutionContext) extends (Try[JsValue] => Unit) {

  def getStatusId(json: JsValue): Option[Long] =
    if ((json \ "type").as[String] == "message")
      Some(((json \ "message" \ "attachments")(0) \ "fallback").as[String].toLong)
    else
      None

  def onMessage(json: JsValue): Unit =
    if ((json \ "user").asOpt[String].exists(_ == user.slackId)) {
      val text = (json \ "text")
        .as[String]
        .replace(s"<@${user.slackId}>", s"@${user.twitterName}")
        .replaceAll("<[@#!]\\w+>", "")
      try {
        twitter.updateStatus(text)
      } catch {
        case e: TwitterException => slack.chat.postMessage(Twilack.channel, e.getMessage)
      }
    }

  def onSuccess(json: JsValue): Unit =
    try {
      println(json)
      (json \ "type").asOpt[String].foreach {
        case "message" => onMessage(json)
        case "star_added" => getStatusId((json \ "item").get).foreach(twitter.createFavorite)
        case "star_removed" => getStatusId((json \ "item").get).foreach(twitter.destroyFavorite)
        case _ =>
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }

  def apply(value: Try[JsValue]): Unit =
    value match {
      case Success(json) => onSuccess(json)
      case Failure(e) => e.printStackTrace()
    }

}
