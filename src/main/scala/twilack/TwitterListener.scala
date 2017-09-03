package twilack

import twitter4j._

class TwitterListener(slack: SlackClient) extends UserStreamAdapter {

  def getStatusURL(status: Status): String = s"https://twitter.com/${status.getUser.getScreenName}/status/${status.getId}"

  def getTimestamp(status: Status): Long = status.getCreatedAt.getTime / 1000

  def entityText(entity: TweetEntity): String =
    entity match {
      case hashtag: HashtagEntity => s"<https://twitter.com/hashtag/${hashtag.getText}|#${hashtag.getText}>"
      case url: URLEntity => s"<${url.getExpandedURL}|${url.getDisplayURL}>"
      case mention: UserMentionEntity => s"<https://twitter.com/${mention.getScreenName}|@${mention.getScreenName}>"
      case _ => entity.getText
    }

  def statusText(status: Status): String = {
    val text = status.getText
    val codePoints = text.codePoints.toArray
    val entities = status.getUserMentionEntities ++ status.getHashtagEntities ++ status.getExtendedMediaEntities ++ status.getURLEntities
    val (index, initText) = entities.sortBy(_.getStart).foldLeft((0, "")) {
      case ((i, acc), entity) =>
        (entity.getEnd, s"${acc}${new String(codePoints, i, entity.getStart - i)}${entityText(entity)}")
    }
    s"${initText}${new String(codePoints, index, codePoints.length - index)}"
  }

  def mediaToAttachment(color: String, media: MediaEntity): Attachment =
    Attachment(
      color = Some(color),
      fallback = Some(media.getId.toString),
      image_url = Some(media.getMediaURLHttps)
    )

  def statusToAttachment(status: Status): Seq[Attachment] = {
    val media = status.getExtendedMediaEntities.map(mediaToAttachment(status.getUser.getProfileBackgroundColor, _))
    Attachment(
      author_name = Some(status.getUser.getName),
      author_link = Some(getStatusURL(status)),
      author_icon = Some(status.getUser.getProfileImageURL),
      color = Some(status.getUser.getProfileBackgroundColor),
      footer = Some(s"<${getStatusURL(status)}|Twitter>"),
      footer_icon = Some("https://g.twimg.com/dev/documentation/image/Twitter_logo_blue_32.png"),
      text = Some(statusText(status)),
      ts = Some(getTimestamp(status))
    ) +: (media ++ Option(status.getQuotedStatus).toSeq.flatMap(statusToAttachment))
  }

  override def onStatus(status: Status): Unit = {
    slack.postMessage(
      "",
      status.getUser.getScreenName,
      status.getUser.getProfileImageURL,
      if (status.isRetweet) statusToAttachment(status.getRetweetedStatus) else statusToAttachment(status)
    )
  }

}
