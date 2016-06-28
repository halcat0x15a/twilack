package twilack.app

import scala.concurrent.ExecutionContext

import twilack.slack.{Attachment, SlackAPI}

import twitter4j._

class TwitterEventHandler(twitter: Twitter, slack: SlackAPI, id: String, name: String, home: String, notifications: String)(implicit ec: ExecutionContext) extends UserStreamAdapter {

  def getAttachments(status: Status): List[Attachment] =
    status.getExtendedMediaEntities.toList.flatMap { media =>
      if (media.getType == "photo" || media.getType == "animated_gif") {
        Some(Attachment(imageUrl = Some(media.getMediaURL)))
      } else {
        None
      }
    }

  def getText(status: Status): String = {
    val text = status.getHashtagEntities.foldLeft(status.getText) { (text, entity) =>
      text.replace(s"#${entity.getText}", s"<https://twitter.com/hashtag/${entity.getText}|#${entity.getText}>")
    }
    (status.getExtendedMediaEntities ++ status.getURLEntities).foldLeft(text) { (text, entity) =>
      text.replace(entity.getURL, entity.getExpandedURL)
    }.replace(s"@${twitter.getScreenName}", s"<@${id}>")
  }

  def getAttachment(id: String, status: Status): Attachment =
    Attachment(
      fallback = Some(id),
      authorName = Some(status.getUser.getScreenName),
      authorIcon = Some(status.getUser.getProfileImageURL),
      text = Some(getText(status))
    )

  def postMessage(channel: String, user: User, attachments: List[Attachment]): Unit =
    slack.postMessage(
      channel,
      "",
      username = Some(user.getScreenName),
      asUser = Some(false),
      iconUrl = Some(user.getProfileImageURL),
      attachments = Some(attachments)
    )

  override def onFavorite(source: User, target: User, favoritedStatus: Status) = {
    if (target.getId == twitter.getId) {
      val attachment = getAttachment(favoritedStatus.getId.toString, favoritedStatus)
      postMessage(notifications, source, attachment +: getAttachments(favoritedStatus))
    }
  }

  override def onStatus(status: Status) = {
    if (status.isRetweet) {
      val retweeted = status.getRetweetedStatus
      val attachment = getAttachment(status.getId.toString, retweeted)
      postMessage(home, status.getUser, attachment +: getAttachments(retweeted))
    } else {
      val attachment = Attachment(
        fallback = Some(status.getId.toString),
        pretext = Some(getText(status))
      )
      postMessage(home, status.getUser, attachment +: getAttachments(status))
    }
  }

}
