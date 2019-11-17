package com.swissquote.lauzhack.evolution.sq.team;

import java.util

import scala.BigDecimal
import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Currency, Price, Trade}
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.ta4j.core.BaseBarSeriesBuilder

import scala.collection.mutable


class ScalaBBook extends BBook {

  def getIndicators = {
    var a = new SMA(8)
    var b = new SMA(16)
    var c = new EMA(4, 12)
    var d = new EMA(9, 17)
    List(a, b, new SMA(32), c, d, new MACD(a, b), new MACD(c, d), new RSI())
  }

  private var bank: Bank = _
  private var trainingMode = false

  /**
   * A price table with all possible pairs of price changes and indicators
   */
  private var priceTable = new mutable.HashMap[(Currency, Currency), List[Indicator]]()
  private var allPrices = new mutable.MutableList[Price]()

  /**
   * Windows using circular first in first out queue which updates automatically
   * windowSize is variable
   */
  private var windowSize = 5

  private var window =  new CircularFifoQueue[List[BigDecimal]](windowSize)
  private var priceWindow = new CircularFifoQueue[Price](windowSize)

  /**
   * Holds the preprocessed dataset
   */
  private var dataSet = new mutable.MutableList[INDArray]
  private var iterator = 0;

  priceTable((Currency.EUR, Currency.CHF)) = getIndicators
  priceTable((Currency.JPY, Currency.CHF)) = getIndicators
  priceTable((Currency.USD, Currency.CHF)) = getIndicators
  priceTable((Currency.GBP, Currency.CHF)) = getIndicators

  override def onInit(): Unit = {
    var builder = new BaseBarSeriesBuilder

    // Start by buying some cash. Don't search for more logic here: numbers are just random.
    bank.buy(new Trade(Currency.EUR, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new java.math.BigDecimal(1000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new java.math.BigDecimal(100000)))
  }

  override def onTrade(trade: Trade): Unit = {
    if (Math.random < 0.05) {
      val coverTrade = new Trade(trade.base, trade.term, trade.quantity.multiply(new java.math.BigDecimal(2)))
      bank.buy(coverTrade)
    }
  }

  override def onPrice(price: Price): Unit = {

    if (!trainingMode) {
//      println("Not training mode")

      if(window.isAtFullCapacity) {
//        println("Window is ready...")
        val data = preprocessData(window, BigDecimal(price.rate) > priceWindow.peek().rate)
        // Feed to the network
      }

      val indicators = priceTable((price.base, price.term))
      window.add(indicators map { _.price(price.rate) })
      priceWindow.add(price)

    }

    allPrices += price
    iterator += 1

    if (iterator == 5000) {

    }
  }

  def preprocessData(window: CircularFifoQueue[List[BigDecimal]], rising: Boolean): INDArray = {

    val arraysToStack = new util.ArrayList[INDArray]()
    val newestEl = window.get(windowSize - 1)

    var i = 0

    window.forEach((el) => {
      var doubleArr = new Array[Double](newestEl.length)
      var ref = windowSize

      el.zipWithIndex.foreach { case(e, j) => {
        doubleArr.update(j, (e / newestEl(j)).toDouble)
      }}

      arraysToStack.add(Nd4j.create(doubleArr))
      println("NICE" + doubleArr)
    })

    val data = Nd4j.vstack(arraysToStack)

    dataSet += data
    i += 1
    data
  }

  override def setBank(bank: Bank): Unit = {
    this.bank = bank;
  }
}