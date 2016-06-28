package twilack.slack

case class SlackAPIException(message: String) extends Exception(message)
