package twilack

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._

case class TwilackConfig(slackToken: String, twitterToken: String, twitterSecret: String, slackChannel: String = "twilack") {

  def toMap: Map[String, String] =
    Map(
      "slack.token" -> slackToken,
      "slack.channel" -> slackChannel,
      "twitter.token" -> twitterToken,
      "twitter.secret" -> twitterSecret
    )

  def toConfig: Config =
    ConfigFactory.parseMap(toMap.asJava)

  def render: String = toConfig.root.render

}

object TwilackConfig {

  def fromConfig(conf: Config): TwilackConfig =
    TwilackConfig(
      conf.getString("slack.token"),
      conf.getString("twitter.token"),
      conf.getString("twitter.secret"),
      conf.getString("slack.channel")
    )

}
