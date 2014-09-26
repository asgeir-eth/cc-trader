package com.cctrader.systems.ann.forwardIndicator

import akka.actor.Props
import com.cctrader.TradingSystemActor
import com.cctrader.data.{MarketDataSet, Signal, Signaler, TSSettings}
import com.cctrader.indicators.machin.ANNOnePeriodAhead
import com.cctrader.indicators.technical.{RateOfChange, RelativeStrengthIndex}
import com.typesafe.config.ConfigFactory

/**
 *
 */
class ForwardIndicatorsTSActor(marketDataSetIn: MarketDataSet, signalWriterIn: Signaler, settingPathIn: String) extends {
  var marketDataSet = marketDataSetIn
  val signalWriter = signalWriterIn
  val settingPath = settingPathIn
} with TradingSystemActor {

  val relativeStrengthIndex: RelativeStrengthIndex = new RelativeStrengthIndex(10)

  var count = 0
  val ann = new ForwardIndicator(settingPath)
  var lastPredict:Double = 0


  /**
   * Train the system.
   * If the system does not need training, return 0
   * @return timestamp in milliseconds for training duration. Timestamp at end of training - start timestamp.
   */
  override def train(trainingMarketDataSet: MarketDataSet): Long = {
    val startTrainingTime = System.currentTimeMillis()
    ann.train(trainingMarketDataSet)
    val endTrainingTime = System.currentTimeMillis()
    endTrainingTime - startTrainingTime
  }
  val roccer = new RateOfChange(11)
  var hasTrade = false
  /**
   * Evaluate new dataPoint.
   * Should be of the same granularity as the training set.
   * @return BUY, SELL or HOLD signal
   */
  override def newDataPoint() {
    val roc = roccer(marketDataSet.size-1, marketDataSet)
    val predict = ann(marketDataSet)
    println("roc:" + predict)
    if ( roc > thresholdLong || predict > thresholdLong) {
      goLong
      hasTrade = true
    }
    else if ( roc < thresholdShort || predict < thresholdShort) {
      goShort
      hasTrade = true
    }
    if (hasTrade) {
      if (( roc < thresholdCloseLong) && signalWriter.status == Signal.LONG) {
        goClose
      }
      else if (( roc > thresholdCloseShort) && signalWriter.status == Signal.SHORT) {
        goClose
      }
    }

    /*
    val rsiToDay = relativeStrengthIndex(marketDataSet.size-1, marketDataSet)
    val prediction = ann(marketDataSet)
    println("Prediction: " + prediction)
    println("RSI today: " + rsiToDay)

    if (signalWriter.status == Signal.SHORT && rsiToDay > 50) {
      goClose
    }
    else if (signalWriter.status == Signal.LONG && rsiToDay < 50) {
      goClose
    }

    if (prediction > thresholdLong || rsiToDay > 70) { //0.4
        goLong
    }
    else if (prediction < thresholdShort || rsiToDay <  30){ // 0.4
        goShort
    }
    */
  }
}

object ForwardIndicatorsTSActor {
  def props(trainingMarketDataSet: MarketDataSet, signalWriterIn: Signaler, tsSetting: String): Props =
    Props(new ForwardIndicatorsTSActor(trainingMarketDataSet, signalWriterIn, tsSetting))
}