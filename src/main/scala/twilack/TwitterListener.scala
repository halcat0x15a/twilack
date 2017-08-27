package twilack

import twitter4j.{Status, UserStreamAdapter}

class TwitterListener(slack: SlackClient) extends UserStreamAdapter {

  def getStatusURL(status: Status): String =
    s"https://twitter.com/${status.getUser.getScreenName}/status/${status.getId}"

  override def onStatus(status: Status): Unit = {
    slack.postMessage(
      s"<${getStatusURL(status)}| >",
      status.getUser.getScreenName,
      status.getUser.getProfileImageURL
    )
  }

}
