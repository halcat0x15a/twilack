package twilack.app

import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

import twilack.slack.SlackAPI

import twitter4j.{Twitter, TwitterException}

class SlackEventHandler(twitter: Twitter, slack: SlackAPI, user: TwilackUser)(implicit ec: ExecutionContext) extends (Try[JsValue] => Unit) {

  def getStatusId(json: JsValue): Option[Long] =
    for {
      tpe <- (json \ "type").asOpt[String]
      if tpe == "message"
      fallback <- ((json \ "message" \ "attachments")(0) \ "fallback").asOpt[String]
      id <- Try(fallback.toLong).toOption
    } yield id

  def onMessage(json: JsValue): Unit =
    for {
      u <- (json \ "user").asOpt[String]
      if u == user.slackId
      text <- (json \ "text").asOpt[String]
    } try {
      val status = text.replace(s"<@${user.slackId}>", s"@${user.twitterName}").replaceAll("<[@#!]\\w+>", "")
      twitter.updateStatus(status)
    } catch {
      case e: TwitterException =>
        slack.chat.postMessage(Twilack.channel, e.getMessage)
    }

  def onSuccess(json: JsValue): Unit =
    (json \ "type").asOpt[String].foreach {
      case "message" => onMessage(json)
      case "star_added" => getStatusId((json \ "item").get).foreach(twitter.createFavorite)
      case "star_removed" => getStatusId((json \ "item").get).foreach(twitter.destroyFavorite)
      case _ =>
    }

  def apply(value: Try[JsValue]): Unit =
    value match {
      case Success(json) => onSuccess(json)
      case Failure(e) => e.printStackTrace()
    }

}
