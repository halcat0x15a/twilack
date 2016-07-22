package twilack.app

import scala.collection.breakOut
import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.util.matching.Regex
import twilack.slack.{Attachment, SlackAPI}
import twitter4j._

class TwitterEventHandler(twitter: Twitter, slack: SlackAPI, user: TwilackUser)(implicit ec: ExecutionContext) extends UserStreamAdapter {

  val StatusURL: Regex = """https://twitter.com/(\w+)/status/(\d+)""".r

  def getMediaURLs(status: Status): List[String] = {
    val entities = status.getMediaEntities ++ status.getExtendedMediaEntities
    val urls = entities.map(_.getMediaURLHttps)(breakOut): List[String]
    urls.distinct
  }

  def getText(status: Status): String = {
    val text = status.getText
    val entities = status.getUserMentionEntities ++ status.getExtendedMediaEntities ++ status.getURLEntities ++ status.getHashtagEntities
    val (n, init) = entities.sortBy(_.getStart).foldLeft((0, "")) {
      case ((n, acc), entity) =>
        val entityText = entity match {
          case entity: HashtagEntity => s"<https://twitter.com/hashtag/${entity.getText}|#${entity.getText}>"
          case entity: URLEntity => s"<${entity.getExpandedURL}|${entity.getDisplayURL}>"
          case entity: UserMentionEntity => s"<https://twitter.com/${entity.getScreenName}|@${entity.getScreenName}>"
          case _ => entity.getText
        }
        (entity.getEnd, s"${acc}${text.substring(n, entity.getStart)}${entityText}")
    }
    s"${init}${text.substring(n)}"
  }

  def getStatuses(status: Status): List[Status] =
    status.getURLEntities.toList.flatMap(_.getExpandedURL match {
      case StatusURL(_, id) => Try(twitter.showStatus(id.toLong)).toOption
      case _ => None
    })

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

  def getAttachments(status: Status): List[Attachment] = {
    val attachment = Attachment()
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

  def postMessage(header: String, status: Status): Unit = {
    slack.chat.postMessage(
      Twilack.channel,
      s"${header}\n${getText(status)}",
      username = Some(status.getUser.getScreenName),
      asUser = Some(false),
      iconUrl = Some(status.getUser.getProfileImageURL),
      attachments = Some(getAttachments(status))
    ).onFailure {
      case e: Throwable => e.printStackTrace()
    }
  }

  override def onStatus(status: Status): Unit = {
    val userName = s"<${getUserURL(status)}|${status.getUser.getName}>"
    if (status.isRetweet) {
      val header = s"${userName} <${getStatusURL(status)}|retweeted>"
      val retweeted = status.getRetweetedStatus
      postMessage(header, retweeted)
    } else {
      val header = s"${userName} <${getStatusURL(status)}|tweeted>"
      postMessage(header, status)
    }
  }

}
