package modules

import java.net.URLEncoder

import app.messaging.{ChatMessage, IrcMessage}
import modules.jsonserde.MagicCard
import play.api.libs.json.{JsError, JsSuccess, JsArray, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalaj.http.{Http, HttpRequest}

import modules.jsonserde.MagicCard._

import scalabotlib.db._

/**
  * Created by Ashwin on 8/6/18.
  */
class ScryfallModule(callback: IrcMessage => Unit) extends AsyncModule(callback) {
  val singleCard = "mtg (.*)$".r
  val cardPrice = "mtgprice (.*)$".r
  val mtgAll = "mtgall(\\d*) (.*)$".r
  override val helpBlurb = Some("scryfall (mtg <query>, mtgall <query>, mtgall<number> <query>")
  def targetCard(cardFuture: Future[List[MagicCard]], callback : MagicCard => ChatMessage, user : String, channel : String, query : String) = {
    cardFuture.map { cards =>
      cards.find(_.name.toLowerCase == query.trim.toLowerCase).orElse(cards.headOption).map(callback).orElse(Some(ChatMessage(user, channel, "You done goofed.")))
    }
  }
  override val asyncBehavior: PartialFunction[IrcMessage, Future[Option[IrcMessage]]] = {

    case ChatMessage(user, channel, singleCard(query: String))  =>
      targetCard(getCardsForQuery(query), x => ChatMessage(user, channel, x.toString), user, channel, query)

    case ChatMessage(user, channel, cardPrice(query: String))  =>
      targetCard(getCardsForQuery(query), x => ChatMessage(user, channel, x.toStringPrice), user, channel, query)

    case ChatMessage(user, channel, mtgAll(count: String, query: String)) =>
      val cardFuture = getCardsForQuery(query)
      val countInt = Try(count.toInt).getOrElse(10)
      cardFuture.map { cards =>
        Some(ChatMessage(user, channel, s"${cards.length} found: ${cards.take(countInt).map(_.name).mkString(sep = "; ")} ${if( cards.length > countInt ) {
                s"(${cards.length - countInt} more found, use mtgall<number> to get more)"
              } else {
                ""
              }
            }"
          )
        )
      }
  }

  // return a future in case the query is slow. bot shouldn't hang on this.
  def getCardsForQuery(query: String): Future[List[MagicCard]] = {
    Future({
      val oneWordRegex = "(\\w+)".r
      val cleanedQuery = query match {
        case oneWordRegex(_) => "\"" + query + "\""
        case _ => query
      }
      println(s"INFO: $query") // TODO MAKE LOGGR
      val rtry = Option(getCacheValueTimeout("cards",query))
      val resp : String = rtry match {
        case Some(i : String) => i
        case None => cacheValue("cards", query, Http(url = "https://api.scryfall.com/cards/search").param(key = "q", cleanedQuery).asString.body)
      }
      val json = Json.parse(resp)
      ((json \ "data").validate[JsArray].asOpt map {
        case JsArray(seq) =>
          seq.flatMap (_.validate[MagicCard] match {
            case s: JsSuccess[MagicCard] => s.asOpt
            case f: JsError =>
              println(f.errors)
              None
          }).toList
        case _ => Nil
      }).getOrElse(Nil)
    })
  }
}