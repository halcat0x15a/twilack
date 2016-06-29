package twilack.app

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import java.io.File

import scala.io.StdIn

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
    OAuthClient.auth(twitter, confFile.toPath)
  }
  val accessToken = new AccessToken(conf.twitterToken, conf.twitterSecret)
  twitter.setOAuthAccessToken(accessToken)
  twitterStream.setOAuthAccessToken(accessToken)
  val api = SlackAPI(conf.slackToken)
  val id = "U1L9JMNRX"
  val name = "halcat0x15a"
  val home = "#general"
  val notifications = "#random"
  val rtm = SlackRTM(api)(new SlackEventHandler(twitter, api, id, name, home))
  twitterStream.addListener(new TwitterEventHandler(twitter, api, id, name, home, notifications))
  twitterStream.user()
}
