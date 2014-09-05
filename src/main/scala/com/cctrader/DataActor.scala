package com.cctrader

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import com.cctrader.data._
import com.cctrader.dbtables._
import com.typesafe.config.ConfigFactory

import scala.slick.driver.PostgresDriver.simple._
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.jdbc.{ResultSetConcurrency, StaticQuery => Q}

/**
 *
 */
class DataActor extends Actor with ActorLogging {

  val config = ConfigFactory.load()
  val databaseFactory = Database.forURL(
    url = "jdbc:postgresql://" + config.getString("postgres.host") + ":" + config.getString("postgres.port") + "/" + config
      .getString("postgres.dbname"),
    driver = config.getString("postgres.driver"),
    user = config.getString("postgres.user"),
    password = config.getString("postgres.password"))

  implicit val session: Session = databaseFactory.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)

  def getDataFromDB(marketDataSettings: MarketDataSettings): MarketDataSet = {
    val table = TableQuery[InstrumentTable]((tag:Tag) => new InstrumentTable(tag, marketDataSettings.instrument))

    val startTime: Date = {
      val firstRow = table.filter(x => x.id === 1L).take(1)
      val value = firstRow.firstOption map (x => x.date)
      value.get
    }

    val endTime: Date = {
      val lengthString = table.length.run
      val lastRow = table.filter(x => x.id === lengthString.toLong).take(1)
      val value = lastRow.firstOption map (x => x.date)
      value.get
    }

    log.info("For instrument " + marketDataSettings.instrument + ": startTime:" + startTime + ", endTime:" + endTime)

    if (marketDataSettings.startDate.before(startTime)) {
      log.error("Market data startTime is before startTime in the database. StartTime is: " + marketDataSettings.startDate + " and start time in DB is: " + startTime)
    }
    MarketDataSet(
      table.filter(_.timestamp <= (marketDataSettings.startDate.getTime/1000).toInt).list.reverse.take(marketDataSettings.numberOfHistoricalPoints).toList.reverse,
      marketDataSettings
    )

  }

  override def receive: Receive = {
    case marketDataSettings: MarketDataSettings =>
      log.info("Received: MarketDataSettings: getting data from database and sending back for MarketDataSettings:" + marketDataSettings)
      val thisMarketDataSet = getDataFromDB(marketDataSettings)
      sender ! Initialize(thisMarketDataSet, context.actorOf(LiveDataActor.props(databaseFactory.createSession(), marketDataSettings, thisMarketDataSet.last.id.get)))
      println(thisMarketDataSet)
  }
}

