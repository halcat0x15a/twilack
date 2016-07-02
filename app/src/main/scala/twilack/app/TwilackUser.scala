package twilack.app

case class TwilackUser(
  slackId: String,
  slackName: String,
  slackChannel: String,
  twitterId: Long,
  twitterName: String
)
