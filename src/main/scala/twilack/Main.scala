package twilack

import com.typesafe.config.ConfigFactory
import twitter4j.TwitterStreamFactory
import twitter4j.auth.AccessToken
import java.io.File

object Main extends App {
  val twitter = new TwitterStreamFactory().getInstance()
  twitter.setOAuthConsumer(Twilack.consumerToken, Twilack.consumerSecret)
  val configFile = new File(args.headOption.getOrElse(Twilack.configName))
  if (configFile.exists()) {
    val config = TwilackConfig.fromConfig(ConfigFactory.parseFile(configFile))
    val accessToken = new AccessToken(config.twitterToken, config.twitterSecret)
    twitter.setOAuthAccessToken(accessToken)
    val slack = new SlackClient(config)
    twitter.addListener(new TwitterListener(slack))
    twitter.user()
  } else {
    println(TwilackConfig("slack_access_token", "twitter_access_token", "twitter_secret_token").render)
  }
}
