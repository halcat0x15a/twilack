package twilack

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import slack.api.SlackApiClient
import slack.rtm.SlackRtmClient

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

object Main extends App {
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher
  val conf = ConfigFactory.load()
  val cb = new ConfigurationBuilder()
    .setDebugEnabled(true)
    .setOAuthConsumerKey(conf.getString("twitter.consumer.token"))
    .setOAuthConsumerSecret(conf.getString("twitter.consumer.secret"))
    .setOAuthAccessToken(conf.getString("twitter.access.token"))
    .setOAuthAccessTokenSecret(conf.getString("twitter.access.secret"))
  val config = cb.build()
  val twitter = new TwitterFactory(config).getInstance()
  val twitterStream = new TwitterStreamFactory(config).getInstance()
  val token = conf.getString("slack.access.token")
  val api = SlackApiClient(token)
  val rtm = SlackRtmClient(token)
  val id = rtm.state.self.id
  val name = rtm.state.self.name
  val home = "#general"
  val notifications = "#random"
  rtm.onEvent(new SlackEventHandler(twitter, api, id, name, home))
  twitterStream.addListener(new TwitterEventHandler(twitter, api, id, name, home, notifications))
  twitterStream.user()
}
