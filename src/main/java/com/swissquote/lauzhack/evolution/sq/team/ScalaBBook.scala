package com.swissquote.lauzhack.evolution;

import scala.collection.JavaConverters._
import java.util

import org.apache.commons.collections4.queue.CircularFifoQueue
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Currency, Price, Trade}
import com.swissquote.lauzhack.evolution.sq.team._
import org.nd4j.linalg.primitives.Pair

import scala.collection.mutable


class ScalaBBook extends BBook {

  /**
   * Descriptive names? Nah. Google the acronyms
   * @return
   */
  def getIndicators = {
    var sma = new SMA(8)
    var sma16 = new SMA(16)
    var ema = new EMA(4, 12)
    var ema9 = new EMA(9, 17)
    List(sma, sma16, new SMA(32), ema, ema9, new MACD(sma, sma16), new MACD(ema, ema9), new RSI())
  }

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
   * Holds the preprocessed dataset in batches
   * [ N of [64 x [ 5 x 8, 1 ]]]
   */
  private var batches = new mutable.MutableList[util.ArrayList[Pair[INDArray, INDArray]]]()
  private var iterator = 0;

  var trades = mutable.Queue[Trade]()

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

  var stat = new mutable.HashMap[Currency, (Int, BigDecimal)]()
  prices(Currency.CHF) = (BigDecimal(1), BigDecimal(1))

  stat(Currency.CHF) = (0, BigDecimal(0))
  stat(Currency.EUR) = (0, BigDecimal(0))
  stat(Currency.GBP) = (0, BigDecimal(0))
  stat(Currency.JPY) = (0, BigDecimal(0))
  stat(Currency.USD) = (0, BigDecimal(0))

  private var nn: NeuralModel = _

  override def onInit(): Unit = {

    bank.buy(new Trade(Currency.EUR, Currency.CHF, new java.math.BigDecimal(800000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new java.math.BigDecimal(21000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new java.math.BigDecimal(800000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new java.math.BigDecimal(800000)))

    nn = new NeuralModel();
    nn.buildModel();

    nn.loadModel("./hello.world")
  }

  /**
   * Guess what, finds optimal value.
   * @param ignored
   * @return
   */
  def findOptimalValue(ignored: Currency): Currency = {
    balance.asScala.filter { _._1 != ignored }.maxBy(it => { BigDecimal(it._2) / prices(it._1)._1 })._1
  }

  override def onTrade(trade: Trade): Unit = {

    val quantity = BigDecimal(trade.quantity)
    val (price, markup) = if(trade.base == Currency.CHF) {
      prices(trade.term)
    } else {
      prices(trade.base)
    }

    /**
     * These values and tradeSteps are hyperparamters.
     */
    var value = 100000
    var jpyValue = value*109.88*2 // According to the exchange rate * 2, since the market seems to have japanese bias sometimes
    /**
     * Trade steps actually divide the trade prices, because sometimes
     * it is better to let 100 CHF go, instead of to disrupt the market to failure
     */
    var tradeSteps = 1

    val min = if(trade.base == Currency.JPY) { jpyValue } else { value }
    var from = trade.term
    if(trade.base != Currency.CHF) {

      if(balance.get(Currency.CHF).compareTo(BigDecimal(value)) < 0) {
        from = findOptimalValue(Currency.CHF)
        for (i <- 0 to value by value/tradeSteps) {
          trades.enqueue(new Trade(Currency.CHF, from, BigDecimal((value/tradeSteps) * 1.1).bigDecimal))
        }
      }
    }

    val balanc = balance.get(trade.base)

    from = trade.term
    if(BigDecimal(balanc) < min) {
      if(trade.base == Currency.CHF) {
        from = findOptimalValue(Currency.CHF)
      }

      for (i <- 0 to value by value/tradeSteps) {
        trades.enqueue(new Trade(trade.base, from, BigDecimal((value/tradeSteps) * 1.3).bigDecimal))
      }
    }

    // Dequeue something
    if(trades.nonEmpty) {
      bank.buy(trades.dequeue())
    }

    from = trade.term
    val balanc_after = balance.get(trade.base)
    println("Balance: ", trade.base, balanc, balanc_after)

    val (count, acum) = stat(trade.base)
    stat(trade.base) = (count + 1, acum + trade.quantity)
  }

  override def onPrice(price: Price): Unit = {

    prices(price.base) = (price.rate, price.markup)
    /**
     * Empty the trade queue evenly distributed across price tick, so we don't disrupt the market
     */
    if(trades.nonEmpty) {
      bank.buy(trades.dequeue())
    }
  }

  /**
   * Very hard WIP
   * @param price
   */
  def neuralNetworkMode(price: Price): Unit = {
    calculateIndicators(price)

    allPrices += price

    if (iterator == 5000) {
      //Save data from allPrices
      println("++++++++++++++++++++++++++++")
      println("Loading data has finished...")
      println("++++++++++++++++++++++++++++")

      // println(dataSet)

      println("++++++++++++++++++++++++++++")
      println("Initializing neural network.")
      println("++++++++++++++++++++++++++++")

      var nn = new NeuralModel()
      nn.buildModel()

      println("++++++++++++++++++++++++++++")
      println("Loading model")
      println("++++++++++++++++++++++++++++")
      // nn.loadModel("./hello.world")

      var epochs = 100;
      for (i <- 0 to epochs) {
        for (batch <- batches) {
          println(batch.size())
          nn.fit(batch)
          batches.last.get(0).getKey()
        }
        println(i + "/" + epochs)
      }

      println("++++++++++++++++++++++++++++")
      println("Finished Training")
      println("++++++++++++++++++++++++++++")
      // var pair: mutable.MutableList[Pair[INDArray, INDArray]] = dataSet.flatMap { _.asScala }
      // var x = pair(0).getKey()

      nn.saveModel("./hello.world")
      // println(nn.predict(x.reshape(1,40)).toList.head)
      // Use the prediction
    }
  }

  /**
   * Gets the current price and assembles the current window after calling preprocessing.
   * @param price
   */
  def calculateIndicators(price: Price): Unit = {
      if(areIndicatorsDone() && window.isAtFullCapacity) {
        var verifiedWindow = new CircularFifoQueue[List[BigDecimal]](windowSize)

        window.forEach {(el) => {
          verifiedWindow.add(el.flatten)
        }}

        val data = preprocessData(verifiedWindow, BigDecimal(price.rate) > priceWindow.peek().rate)
        iterator += 1
      }

      val indicators = indicatorTable((price.base, price.term))
      window.add(indicators map { _.price(price.rate) })
      priceWindow.add(price)
  }

  /**
   * Does data processing, matrix concatination, normalization, does pairing with Y values for the ML Algorithm.
   * @param window
   * @param rising
   * @return
   */
  def preprocessData(window: CircularFifoQueue[List[BigDecimal]], rising: Boolean): Pair[INDArray, INDArray] = {

    val arraysToStack = new util.ArrayList[INDArray]()
    val newestEl = window.get(windowSize - 1)

    window.forEach((el) => {
      var doubleArr = new Array[Double](newestEl.length)
      var ref = windowSize

      el.zipWithIndex.foreach { case(e, j) => {
        /**
         *  Normalization, try modified tahn/softmax even though it is kindof obvious it won't work
         */
        // doubleArr.update(j, (e / newestEl(j)).toDouble)
        doubleArr.update(j, e.toDouble)
      }}

      arraysToStack.add(Nd4j.create(doubleArr))
    })

    val data = Nd4j.vstack(arraysToStack)
    val groundTruth = Nd4j.create(Array[Double](if (rising) 1.0 else 0.0))
    val datum = new Pair(data, groundTruth)

    // println("Predicting:")
    // println(nn.predict(data.reshape(1,40)).toList.head)
    // println("##################################")

    val finalDatum = new Pair[INDArray, INDArray](data, groundTruth)

    if(batches.isEmpty) {
      batches += new util.ArrayList[Pair[INDArray, INDArray]]()
    } else {
      if(batches.last.size() == 64) {
        batches += new util.ArrayList[Pair[INDArray, INDArray]]()
      }
    }

    batches.last.add(finalDatum)
    finalDatum
  }

  /**
   * Checks if all the indicators have their prerequisites satisfied.
   * @returns Boolean
   */
  def areIndicatorsDone(): Boolean = window.iterator().asScala.forall { _.forall(_.nonEmpty) }


  /**
   * Steals the bank
   * @param bank
   */
  override def setBank(bank: Bank): Unit = {
    // We cast the interface to the type that implements it
    this.bank = bank.asInstanceOf[b]
    this.balance = this.bank.a
  }
}