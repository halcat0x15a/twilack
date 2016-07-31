package twilack.app

import java.time.{LocalDateTime, ZoneOffset}
import play.api.libs.json.JsValue
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex
import twilack.slack.{SlackAPI, SlackRTM}
import twitter4j.{Twitter, TwitterException}

class SlackEventHandler(twitter: Twitter, api: SlackAPI, rtm: SlackRTM, user: TwilackUser)(implicit ec: ExecutionContext) extends (JsValue => Unit) {

  val StatusId: Regex = """/status/(\d+)""".r

  def getStatusId(json: JsValue): Option[Long] =
    for {
      tpe <- (json \ "type").asOpt[String]
      if tpe == "message"
      text <- (json \ "message" \ "text").asOpt[String]
      header <- text.split("\n").headOption
      status <- StatusId.findFirstMatchIn(header)
      id <- Try(status.group(1).toLong).toOption
    } yield id

  def tsToDateTime(ts: Int): LocalDateTime =
    LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC)

  def tsToDateTime(ts: String): Option[LocalDateTime] =
    Try {
      val pair = ts.split("\\.")
      LocalDateTime.ofEpochSecond(pair(0).toLong, pair(1).toInt * 1000, ZoneOffset.UTC)
    }.toOption

  def onMessage(json: JsValue): Unit =
    rtm.state().onSuccess {
      case state =>
        for {
          cache <- (state \ "cache_ts").asOpt[Int]
          ts <- (json \ "ts").asOpt[String]
          ts <- tsToDateTime(ts)
          if ts.isAfter(tsToDateTime(cache))
          usr <- (json \ "user").asOpt[String]
          if usr == user.slackId
          chan <- (json \ "channel").asOpt[String]
          if chan == user.slackChannel
          text <- (json \ "text").asOpt[String]
        } try {
          val status = text.replace(s"<@${user.slackId}>", s"@${user.twitterName}").replaceAll("<[@#!]\\w+>", "")
          twitter.updateStatus(status)
        } catch {
          case e: TwitterException =>
            api.chat.postMessage(Twilack.channel, e.getMessage)
        }
    }

  def apply(json: JsValue): Unit =
    (json \ "type").asOpt[String].foreach {
      case "message" => onMessage(json)
      case "star_added" => getStatusId((json \ "item").get).foreach(twitter.createFavorite)
      case "star_removed" => getStatusId((json \ "item").get).foreach(twitter.destroyFavorite)
      case _ =>
    }

}
