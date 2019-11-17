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
  private var prices = new mutable.HashMap[Currency, (BigDecimal, BigDecimal)]()
  prices(Currency.CHF) = (BigDecimal(1), BigDecimal(1))
  var stat = new mutable.HashMap[Currency, (Int, BigDecimal)]()

  stat(Currency.CHF) = (0, BigDecimal(0))
  stat(Currency.EUR) = (0, BigDecimal(0))
  stat(Currency.GBP) = (0, BigDecimal(0))
  stat(Currency.JPY) = (0, BigDecimal(0))
  stat(Currency.USD) = (0, BigDecimal(0))

  override def onInit(): Unit = {

    bank.buy(new Trade(Currency.EUR, Currency.CHF, new java.math.BigDecimal(800000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new java.math.BigDecimal(21000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new java.math.BigDecimal(800000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new java.math.BigDecimal(800000)))

    println(balance)
  }

  override def onTrade(trade: Trade): Unit = {

    var trades = mutable.Queue[Trade]()

    val quantity = BigDecimal(trade.quantity)
    val (price, markup) = if(trade.base == Currency.CHF) {
      prices(trade.term)
    }else{
      prices(trade.base)
    }

    val min = if(trade.base == Currency.JPY) { 10000000 } else { 100000 }
    var from = trade.term
    if(trade.base != Currency.CHF) {

      if(balance.get(Currency.CHF).compareTo(BigDecimal(1000000)) < 0) {
        from = balance.asScala.filter { _._1 != Currency.CHF }.maxBy(it => { BigDecimal(it._2) / prices(it._1)._1 })._1
        trades.enqueue(new Trade(Currency.CHF, from, (BigDecimal(min / 2).abs.bigDecimal)))
      }
    }

    val balanc = balance.get(trade.base)

    from = trade.term
    if(BigDecimal(balanc) < min){
      if(trade.base == Currency.CHF) {
        from = balance.asScala.maxBy(it => { BigDecimal(it._2) / prices(it._1)._1 })._1
      }
      trades.enqueue(new Trade(trade.base, from, (BigDecimal(min / 2).abs.bigDecimal)))
    }

    from = trade.term
    val balanc_after = balance.get(trade.base)
    println(trade.base, balanc, balanc_after)
//    println((trade.base, trade.term, trade.quantity), balance)
    while(trades.nonEmpty) {
      bank.buy(trades.dequeue())
    }

    val (count, acum) = stat(trade.base)
    stat(trade.base) = (count + 1, acum + trade.quantity)

  }

  override def onPrice(price: Price): Unit = {

    prices(price.base) = (price.rate, price.markup)



    return;
    if (!trainingMode) {
//      println("Not training mode")

      if(areIndicatorsDone() && window.isAtFullCapacity) {
//        println("Window is ready...")
        var verifiedWindow = new CircularFifoQueue[List[BigDecimal]](windowSize)

        window.forEach {(el) => {
          verifiedWindow.add(el.flatten)
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
//      println("NICE:")
//      println(Nd4j.create(doubleArr))
    })

    val data = Nd4j.vstack(arraysToStack)

    dataSet += data
    i += 1
    data
  }

  def areIndicatorsDone(): Boolean =
    window.iterator().asScala.forall { _.forall(_.nonEmpty) }

  override def setBank(bank: Bank): Unit = {
    // We cast the interface to the type that implements it
    this.bank = bank.asInstanceOf[b]
    this.balance = this.bank.a
  }
}