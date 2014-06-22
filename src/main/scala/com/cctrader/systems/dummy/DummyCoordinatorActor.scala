package com.cctrader.systems.dummy

import java.util.Date

import akka.actor.{ActorRef, Props}
import com.cctrader.data.{MarketDataSet, CurrencyPair, Exchange, Granularity}
import com.cctrader.{DataReady, MarketDataSettings, TSCoordinatorActor}

/**
 *
 */
class DummyCoordinatorActor(dataActorIn: ActorRef, dataAvailableIn: DataReady) extends {
  val name = "Dummy"
  val dataAvailable = dataAvailableIn
  val dataActor = dataActorIn
  var tradingSystemTime = new Date(1339539816L * 1000L)
  val numberOfLivePointsAtTheTimeForBackTest = 100
  var transferToNextSystemDate: Date = new Date(0)
  val sigmoidNormalizerScale = 20
  var nextSystemReady: Boolean = false
  val tsNumberOfPointsToProcessBeforeStartTrainingNewSystem = 100

  val marketDataSettings = MarketDataSettings(
    startDate = tradingSystemTime,
    numberOfHistoricalPoints = 100,
    granularity = Granularity.day,
    currencyPair = CurrencyPair.BTC_USD,
    exchange = Exchange.bitstamp,
    PriceChangeScale = 50,
    VolumeChangeScale = 1000,
    MinPrice = 0,
    MaxPrice = 1500,
    MinVolume = 0,
    MaxVolume = 1000000
  )

} with TSCoordinatorActor {

  def tsProps = DummyTSActor.props(MarketDataSet(marketDataSet.iterator.toList, marketDataSet.settings), signalWriter)

  var tradingSystemActor: ActorRef = _
  val signalWriter = new SignalWriter(name + "trades")
  var nextTradingSystem: ActorRef = _
}

object DummyCoordinatorActor {
  def props(dataActor: ActorRef, dataReady: DataReady): Props =
    Props(new DummyCoordinatorActor(dataActor, dataReady))
}