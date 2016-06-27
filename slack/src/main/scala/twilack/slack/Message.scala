package twilack.slack

sealed abstract class Message

object Message {

  case class Text(value: String) extends Message

  case object Close extends Message

}
