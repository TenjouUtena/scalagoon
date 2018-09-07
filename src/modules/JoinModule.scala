package modules

import app.messaging._

import scala.util.matching.Regex

/**
  * Created by Ashwin on 8/5/18.
  */
class JoinModule(channel: String) extends IrcModule {
  override def func: PartialFunction[IrcMessage, Option[IrcMessage]] = {
    case ServerMessage(4, _) => Some(JoinMessage(channel))
  }
}

class BadNickModule(nick: String) extends IrcModule {
  override def func: PartialFunction[IrcMessage, Option[IrcMessage]] = {
    case ServerMessage(433, _) => Some(NickMessage(nick + "_"))
  }
}
