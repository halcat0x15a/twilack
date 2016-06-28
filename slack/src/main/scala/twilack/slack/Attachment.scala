package twilack.slack

import play.api.libs.json.{Format, Json, JsPath}
import play.api.libs.functional.syntax._

case class Attachment(
  fallback: Option[String] = None,
  color: Option[String] = None,
  pretext: Option[String] = None,
  authorName: Option[String] = None,
  authorLink: Option[String] = None,
  authorIcon: Option[String] = None,
  fields: Option[List[Attachment.Field]] = None,
  title: Option[String] = None,
  titleLink: Option[String] = None,
  text: Option[String] = None,
  imageUrl: Option[String] = None,
  thumbUrl: Option[String] = None,
  footer: Option[String] = None,
  footerIcon: Option[String] = None,
  ts: Option[String] = None
)

object Attachment {

  case class Field(title: Option[String], value: Option[String], short: Option[String])

  object Field {

    implicit val format: Format[Field] = Json.format[Field]

  }

  implicit val format: Format[Attachment] = (
    (JsPath \ "fallback").formatNullable[String] and
    (JsPath \ "color").formatNullable[String] and
    (JsPath \ "pretext").formatNullable[String] and
    (JsPath \ "author_name").formatNullable[String] and
    (JsPath \ "author_link").formatNullable[String] and
    (JsPath \ "author_icon").formatNullable[String] and
    (JsPath \ "fields").formatNullable[List[Attachment.Field]] and
    (JsPath \ "title").formatNullable[String] and
    (JsPath \ "title_link").formatNullable[String] and
    (JsPath \ "text").formatNullable[String] and
    (JsPath \ "image_url").formatNullable[String] and
    (JsPath \ "thumb_url").formatNullable[String] and
    (JsPath \ "footer").formatNullable[String] and
    (JsPath \ "footer_icon").formatNullable[String] and
    (JsPath \ "ts").formatNullable[String]
  )(Attachment.apply, unlift(Attachment.unapply))

}

