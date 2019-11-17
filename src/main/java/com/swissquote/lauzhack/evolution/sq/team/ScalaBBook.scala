package com.swissquote.lauzhack.evolution;

import scala.BigDecimal
import java.util
import scala.collection.JavaConverters._

import java.util

import scala.BigDecimal
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.ta4j.core.BaseBarSeriesBuilder


import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Currency, Price, Trade}
import com.swissquote.lauzhack.evolution.b
import com.swissquote.lauzhack.evolution.sq.team._

import scala.collection.mutable


class ScalaBBook extends BBook {

  /**
   * Descriptive names? Nah.
   * @return
   */
  def getIndicators = {
    var a = new SMA(8)
    var b = new SMA(16)
    var c = new EMA(4, 12)
    var d = new EMA(9, 17)
    List(a, b, new SMA(32), c, d, new MACD(a, b), new MACD(c, d), new RSI())
  }

  /**
   * Are we training the algorithm?
   */
  private var trainingMode = false

  /**
   * A price table with all possible pairs of price changes and indicators
   */
  private var indicatorTable = new mutable.HashMap[(Currency, Currency), List[Indicator]]()
  private var allPrices = new mutable.MutableList[Price]()

  /**
   * Windows using circular first in first out queue which updates automatically
   * windowSize is variable
   */
  private var windowSize = 5

  private var window =  new CircularFifoQueue[List[Option[BigDecimal]]](windowSize)
  private var priceWindow = new CircularFifoQueue[Price](windowSize)

  /**
   * Holds the preprocessed dataset
   */
  private var dataSet = new mutable.MutableList[INDArray]
  // Debug purposes
  private var iterator = 0;

  indicatorTable((Currency.EUR, Currency.CHF)) = getIndicators
  indicatorTable((Currency.JPY, Currency.CHF)) = getIndicators
  indicatorTable((Currency.USD, Currency.CHF)) = getIndicators
  indicatorTable((Currency.GBP, Currency.CHF)) = getIndicators

  /**
   * Too lazy to calculate our balance so why not explicitly cast and steal it?
   */
  private var bank: b = _
  private var balance: util.Map[Currency, java.math.BigDecimal] = _
  private var prices = new mutable.HashMap[(Currency, Currency), (BigDecimal, BigDecimal)]()

  override def onInit(): Unit = {

    println(balance)
    bank.buy(new Trade(Currency.EUR, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new java.math.BigDecimal(1000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new java.math.BigDecimal(100000)))
  }

  override def onTrade(trade: Trade): Unit = {
    if (Math.random < 0.05) {
      var quantity = BigDecimal(trade.quantity)
      var (price, markup) = prices((trade.base, trade.term))

      if ((BigDecimal(balance.get(trade.base)) - (quantity * price * 2 * (1 + markup))) <= 0) {

      } else {

        val coverTrade = new Trade(trade.base, trade.term, (quantity * 1.2).bigDecimal)
        bank.buy(coverTrade)
      }


      println(balance)
    }
  }

  override def onPrice(price: Price): Unit = {

    prices((price.base, price.term)) = (price.rate, price.markup)

    if (!trainingMode) {
      println("Not training mode")

      if(areIndicatorsDone() && window.isAtFullCapacity) {
        println("Window is ready...")
        var verifiedWindow = new CircularFifoQueue[List[BigDecimal]](windowSize)

        window.forEach {(el) => {
          verifiedWindow.add(el flatMap {(el) => {el}})
        }}

        val data = preprocessData(verifiedWindow, BigDecimal(price.rate) > priceWindow.peek().rate)
        // Pair data, then
        // Feed to the networks
      }

      val indicators = indicatorTable((price.base, price.term))
      window.add(indicators map { _.price(price.rate) })
      priceWindow.add(price)

    }

    allPrices += price
    iterator += 1

    if (iterator == 5000) {
      //Save data from allPrices
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

  def areIndicatorsDone(): Boolean = {
    window.forEach {(el) => {
      if (el.isEmpty) {
        return false
      }
      println("WINDOWWWWS:" + el);
    }}

    true
  }

  override def setBank(bank: Bank): Unit = {
    // We cast the interface to the type that implements it
    this.bank = bank.asInstanceOf[b]
    this.balance = this.bank.a
  }
}