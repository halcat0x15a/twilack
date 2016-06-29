package twilack.app

import akka.actor.ActorSystem

import com.typesafe.config.ConfigFactory

import javafx.application.Application

import scala.io.StdIn

import twilack.slack.{SlackAPI, SlackRTM}

import twitter4j._
import twitter4j.conf.ConfigurationBuilder

object Main extends App {
  implicit val system = ActorSystem("slack")
  implicit val ec = system.dispatcher
  Application.launch(classOf[OAuthClient])
  /*val code = StdIn.readLine("code: ")
  val api = SlackAPI.access(clientId, clientSecret, code)
  val consumerToken = "bT4HNS87r4ja9m9SdXIOBzsTL"
  val consumerSecret = "0wIAkacjcqha1POVQSu8Uvwtlb9xnBSYdlGzvHUk3aHPLKCOzW"
  val twitter = new TwitterFactory().getInstance()
  twitter.setOAuthConsumer(consumerToken, consumerSecret)
  val requestToken = twitter.getOAuthRequestToken
  println(requestToken.getAuthorizationURL)
  val pin = StdIn.readLine("code: ")
  val accessToken = twitter.getOAuthAccessToken(requestToken, pin)
  val twitterStream = new TwitterStreamFactory().getInstance()
  twitterStream.setOAuthConsumer(consumerToken, consumerSecret)
  twitterStream.setOAuthAccessToken(accessToken)
  val id = "U1L9JMNRX"
  val name = "halcat0x15a"
  val home = "#general"
  val notifications = "#random"
  val rtm = SlackRTM(api)(new SlackEventHandler(twitter, api, id, name, home))
  twitterStream.addListener(new TwitterEventHandler(twitter, api, id, name, home, notifications))
  twitterStream.user()*/
}
