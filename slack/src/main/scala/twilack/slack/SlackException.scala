package twilack.slack

case class SlackException(message: String) extends Exception(message)
