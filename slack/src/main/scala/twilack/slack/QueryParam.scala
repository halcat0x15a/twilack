package twilack.slack

import play.api.libs.json.Json

trait QueryParam[A] {

  def apply(value: A): Option[String]

}

object QueryParam {

  implicit val StringParam: QueryParam[String] =
    new QueryParam[String] {
      def apply(value: String) = Some(value)
    }

  implicit val BooleanParam: QueryParam[Boolean] =
    new QueryParam[Boolean] {
      def apply(value: Boolean) = Some(value.toString)
    }

  implicit val AttachmentListParam: QueryParam[List[Attachment]] =
    new QueryParam[List[Attachment]] {
      def apply(value: List[Attachment]) = Some(Json.stringify(Json.toJson(value)))
    }

  implicit def OptionParam[A](implicit A: QueryParam[A]): QueryParam[Option[A]] =
    new QueryParam[Option[A]] {
      def apply(value: Option[A]) = value.flatMap(A.apply)
    }

}
