package twilack.app

import scala.concurrent.ExecutionContext

import twilack.slack.{Attachment, SlackAPI}

import twitter4j._

class TwitterEventHandler(twitter: Twitter, slack: SlackAPI, user: TwilackUser)(implicit ec: ExecutionContext) extends UserStreamAdapter {

  def getAttachments(status: Status): List[Attachment] =
    (status.getMediaEntities ++ status.getExtendedMediaEntities).toList.flatMap { media =>
      println(s"media: $media")
      if (media.getType == "photo" || media.getType == "animated_gif") {
        Some(Attachment(imageUrl = Some(media.getMediaURL)))
      } else {
        None
      }
    }.distinct

  def getText(status: Status): String = {
    val text = status.getHashtagEntities.foldLeft(status.getText) { (text, entity) =>
      text.replace(s"#${entity.getText}", s"<https://twitter.com/hashtag/${entity.getText}|#${entity.getText}>")
    }
    (status.getExtendedMediaEntities ++ status.getURLEntities).foldLeft(text) { (text, entity) =>
      text.replace(entity.getURL, entity.getExpandedURL)
    }.replace(s"@${user.twitterName}", s"<@${user.slackId}>")
  }

  def postMessage(user: User, attachments: List[Attachment]): Unit =
    slack.chat.postMessage(
      Twilack.channel,
      "",
      username = Some(user.getScreenName),
      asUser = Some(false),
      iconUrl = Some(user.getProfileImageURL),
      attachments = Some(attachments)
    )

  def statusToAttachment(status: Status): Attachment =
    statusToAuther(status).copy(text = Some(getText(status)))

  def statusToAuther(status: Status): Attachment =
    Attachment(
      fallback = Some(status.getId.toString),
      authorName = Some(status.getUser.getScreenName),
      authorIcon = Some(status.getUser.getProfileImageURL)
    )

  def statusToPretext(status: Status): Attachment =
    Attachment(
      fallback = Some(status.getId.toString),
      pretext = Some(getText(status))
    )

  override def onStatus(status: Status) = {
    if (status.isRetweet) {
      val retweeted = status.getRetweetedStatus
      println(getAttachments(retweeted))
      postMessage(retweeted.getUser, statusToPretext(retweeted) +: getAttachments(retweeted) :+ statusToAuther(status))
    } else {
      println(getAttachments(status))
      postMessage(status.getUser, statusToPretext(status) +: getAttachments(status))
    }
  }

}
