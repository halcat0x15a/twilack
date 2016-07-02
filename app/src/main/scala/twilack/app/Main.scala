package twilack.app

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import java.io.File

import play.api.libs.json.JsValue

import scala.util.{Failure, Success}

import twilack.slack.{SlackAPI, SlackRTM}

import twitter4j._
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

object Main extends App {
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher
  val twitter = new TwitterFactory().getInstance()
  twitter.setOAuthConsumer(Twilack.consumerToken, Twilack.consumerSecret)
  val twitterStream = new TwitterStreamFactory().getInstance()
  twitterStream.setOAuthConsumer(Twilack.consumerToken, Twilack.consumerSecret)
  val confFile = new File(Twilack.confName)
  val conf = if (confFile.exists()) {
    TwilackConfig.fromConfig(ConfigFactory.parseFile(confFile))
  } else {
    OAuthClient.authorize(twitter, confFile.toPath)
  }
  val accessToken = new AccessToken(conf.twitterToken, conf.twitterSecret)
  twitter.setOAuthAccessToken(accessToken)
  twitterStream.setOAuthAccessToken(accessToken)
  val api = SlackAPI(conf.slackToken)
  SlackRTM.start(api).zip(api.channels.list()).onComplete {
    case Success((rtm, list)) =>
      val channel = (list \ "channels")
        .as[List[JsValue]]
        .map(channel => (channel \ "name").as[String] -> (channel \ "id").as[String])
        .toMap
        .apply(Twilack.channelName)
      val id = (rtm.state \ "self" \ "id").as[String]
      val name = (rtm.state \ "self" \ "name").as[String]
      val user = TwilackUser(id, name, channel, twitter.getId, twitter.getScreenName)
      rtm.onComplete(new SlackEventHandler(twitter, api, user))
      twitterStream.addListener(new TwitterEventHandler(twitter, api, user))
      twitterStream.user()
    case Failure(e) =>
      e.printStackTrace
  }
}
