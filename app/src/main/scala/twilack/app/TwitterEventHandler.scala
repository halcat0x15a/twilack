package twilack.app

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.matching.Regex

import twilack.slack.{Attachment, SlackAPI}

import twitter4j._

class TwitterEventHandler(twitter: Twitter, slack: SlackAPI, user: TwilackUser)(implicit ec: ExecutionContext) extends UserStreamAdapter {

  val StatusURL: Regex = """https://twitter.com/(\w+)/status/(\d+)""".r

  def getMediaURLs(status: Status): List[String] =
    (status.getMediaEntities ++ status.getExtendedMediaEntities).toList.flatMap { media =>
      if (media.getType == "photo" || media.getType == "animated_gif") {
        Some(media.getMediaURLHttps)
      } else {
        None
      }
    }.distinct

  def replaceHashtag(text: String, entities: Seq[HashtagEntity]): String =
    entities.foldLeft(text) { (text, entity) =>
      text.replace(s"#${entity.getText}", s"<https://twitter.com/hashtag/${entity.getText}|#${entity.getText}>")
    }

  def replaceURL(text: String, entities: Seq[URLEntity]): String =
    entities.foldLeft(text) { (text, entity) =>
      text.replace(entity.getURL, s"<${entity.getExpandedURL}|${entity.getDisplayURL}>")
    }

  def replaceScreenName(text: String): String =
    text.replace(s"@${user.twitterName}", s"<@${user.slackId}>")

  def getText(status: Status): String = {
    val replacingHashtag = replaceHashtag(status.getText, status.getHashtagEntities)
    val replacingURL = replaceURL(replacingHashtag, status.getExtendedMediaEntities ++ status.getURLEntities)
    val replacingScreenName = replaceScreenName(replacingURL)
    replacingScreenName
  }

  def getStatuses(status: Status): List[Status] =
    status.getURLEntities.toList.flatMap(_.getExpandedURL match {
      case StatusURL(_, id) => Try(twitter.showStatus(id.toLong)).toOption
      case _ => None
    })

  def postMessage(user: User, attachments: List[Attachment]): Unit =
    slack.chat.postMessage(
      Twilack.channel,
      "",
      username = Some(user.getScreenName),
      asUser = Some(false),
      iconUrl = Some(user.getProfileImageURL),
      attachments = Some(attachments)
    ).onFailure {
      case e: Throwable => e.printStackTrace()
    }

  def attachMedia(media: String, attachment: Attachment = Attachment()): Attachment =
    attachment.copy(
      text = Some(attachment.text.getOrElse("")),
      imageUrl = Some(media)
    )

  def attachStatus(status: Status, attachment: Attachment = Attachment()): Attachment =
    attachment.copy(
      authorName = Some(status.getUser.getScreenName),
      authorIcon = Some(status.getUser.getProfileImageURL),
      text = Some(getText(status))
    )

  def getAttachments(status: Status, header: String): List[Attachment] = {
    val attachment = Attachment(
      fallback = Some(status.getId.toString),
      pretext = Some(s"${header}\n${getText(status)}")
    )
    (getMediaURLs(status), getStatuses(status)) match {
      case (url :: urls, statuses) =>
        attachMedia(url, attachment) :: urls.map(attachMedia(_)) ::: statuses.map(attachStatus(_))
      case (Nil, status :: statuses) =>
        attachStatus(status, attachment) :: statuses.map(attachStatus(_))
      case _ => List(attachment)
    }
  }

  def getUserURL(status: Status): String =
    s"https://twitter.com/${status.getUser.getScreenName}"

  def getStatusURL(status: Status): String =
    s"${getUserURL(status)}/status/${status.getId}"

  override def onStatus(status: Status) = {
    val header = s"<${getUserURL(status)}|${status.getUser.getName}> <${getStatusURL(status)}|tweeted>"
    if (status.isRetweet) {
      val retweeted = status.getRetweetedStatus
      postMessage(retweeted.getUser, getAttachments(retweeted, header))
    } else {
      postMessage(status.getUser, getAttachments(status, header))
    }
  }

}
