package twilack

import com.typesafe.config.ConfigFactory
import twitter4j.TwitterStreamFactory
import twitter4j.auth.AccessToken
import java.io.File

object Main extends App {
  val twitter = new TwitterStreamFactory().getInstance()
  twitter.setOAuthConsumer(Twilack.consumerToken, Twilack.consumerSecret)
  val configFile = new File(sys.props.get("twilack.conf").getOrElse(Twilack.configName))
  val config = if (configFile.exists()) {
    TwilackConfig.fromConfig(ConfigFactory.parseFile(configFile))
  } else {
    OAuthClient.authorize(twitter, configFile.toPath)
  }
  val accessToken = new AccessToken(config.twitterToken, config.twitterSecret)
  twitter.setOAuthAccessToken(accessToken)
  val slack = new SlackClient(config)
  twitter.addListener(new TwitterListener(slack))
  twitter.user()
}
