package twilack.app

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import twilack.slack.{SlackAPI, SlackRTM}

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

object Main extends App {
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher
  val conf = ConfigFactory.load()
  val cb = new ConfigurationBuilder()
    .setOAuthConsumerKey(conf.getString("twitter.consumer.token"))
    .setOAuthConsumerSecret(conf.getString("twitter.consumer.secret"))
    .setOAuthAccessToken(conf.getString("twitter.access.token"))
    .setOAuthAccessTokenSecret(conf.getString("twitter.access.secret"))
  val config = cb.build()
  val twitter = new TwitterFactory(config).getInstance()
  val twitterStream = new TwitterStreamFactory(config).getInstance()
  val token = conf.getString("slack.access.token")
  val id = "U1L9JMNRX"
  val name = "halcat0x15a"
  val home = "#general"
  val notifications = "#random"
  val api = SlackAPI(token)
  val rtm = SlackRTM(api)(new SlackEventHandler(twitter, api, id, name, home))
  twitterStream.addListener(new TwitterEventHandler(twitter, api, id, name, home, notifications))
  twitterStream.user()
}
