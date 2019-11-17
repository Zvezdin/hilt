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
import org.nd4j.linalg.api.buffer.DataBuffer
import org.nd4j.linalg.primitives.Pair

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
  private var dataSet = new mutable.MutableList[util.ArrayList[Pair[INDArray, INDArray]]]()
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
  private var nn: NeuralModel = _

  override def onInit(): Unit = {

    nn = new NeuralModel();
    nn.buildModel();

    nn.loadModel("./hello.world")


    println(balance)
    bank.buy(new Trade(Currency.EUR, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new java.math.BigDecimal(1000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new java.math.BigDecimal(100000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new java.math.BigDecimal(100000)))
  }

  override def onTrade(trade: Trade): Unit = {
    if (Math.random < 0.05) {
      val quantity = BigDecimal(trade.quantity)
      val (price, markup) = if(trade.base == Currency.CHF) {
        prices(trade.term)
      }else{
        prices(trade.base)
      }

      if ((BigDecimal(balance.get(trade.base)) - (quantity * price * 2 * (1 + markup))) <= 0) {

      } else {

        val coverTrade = new Trade(trade.base, trade.term, (quantity * 1.2).bigDecimal)
        bank.buy(coverTrade)
      }

      println(balance)
    }
  }

  override def onPrice(price: Price): Unit = {

    prices(price.base) = (price.rate, price.markup)

    calculateIndicators(price)

    allPrices += price

    if (iterator == 5000) {
      //Save data from allPrices
      println("++++++++++++++++++++++++++++")
      println("Loading data has finished...")
      println("++++++++++++++++++++++++++++")

//      println(dataSet)

      println("++++++++++++++++++++++++++++")
      println("Initializing neural network.")
      println("++++++++++++++++++++++++++++")

      var nn = new NeuralModel()
      nn.buildModel()

      println("++++++++++++++++++++++++++++")
      println("Loading model")
      println("++++++++++++++++++++++++++++")
//      nn.loadModel("./hello.world")

      var epochs = 100;
      for (i <- 0 to epochs) {
          for (el <- dataSet) {
            nn.fit(el)
          }
          println(i + "/" + epochs)
      }

      println("++++++++++++++++++++++++++++")
      println("Finished Training")
      println("++++++++++++++++++++++++++++")
      var pair: mutable.MutableList[Pair[INDArray, INDArray]] = dataSet.flatMap { _.asScala }
      var x = pair(0).getKey()

//      nn.saveModel("./hello.world")
      println(nn.predict(x.reshape(1,40)).toList.head)

      println("Actual answer: " + pair(0).getValue())
      Thread.sleep(100000)
    }
  }

  def calculateIndicators(price: Price): Unit = {
    if (!trainingMode) {

      if(areIndicatorsDone() && window.isAtFullCapacity) {
        var verifiedWindow = new CircularFifoQueue[List[BigDecimal]](windowSize)

        window.forEach {(el) => {
          verifiedWindow.add(el.flatten)
        }}

        val data = preprocessData(verifiedWindow, BigDecimal(price.rate) > priceWindow.peek().rate)
        iterator += 1
        // Pair data, then
        // Feed to the networks
      }

      val indicators = indicatorTable((price.base, price.term))
      window.add(indicators map { _.price(price.rate) })
      priceWindow.add(price)

    }
  }

  def preprocessData(window: CircularFifoQueue[List[BigDecimal]], rising: Boolean): util.ArrayList[Pair[INDArray, INDArray]] = {

    val arraysToStack = new util.ArrayList[INDArray]()
    val newestEl = window.get(windowSize - 1)

    window.forEach((el) => {
      var doubleArr = new Array[Double](newestEl.length)
      var ref = windowSize

      el.zipWithIndex.foreach { case(e, j) => {
        // Normalization, try modified tahn/softmax even though it is kindof obvious it won't work
        // doubleArr.update(j, (e / newestEl(j)).toDouble)
        doubleArr.update(j, e.toDouble)
      }}

      arraysToStack.add(Nd4j.create(doubleArr))
    })

    val data = Nd4j.vstack(arraysToStack)
    val groundTruth = Nd4j.create(Array[Double](if (rising) 1.0 else 0.0))
    val datum = new Pair(data, groundTruth)

//    println("Predicting:")
//    println(nn.predict(data.reshape(1,40)).toList.head)
//    println("##################################")

    val finalDatum = new util.ArrayList[Pair[INDArray, INDArray]]()
    finalDatum.add(datum)
    // If not initialized, initialize
    dataSet += finalDatum
    finalDatum
  }

  def areIndicatorsDone(): Boolean =
    window.iterator().asScala.forall { _.forall(_.nonEmpty) }

  override def setBank(bank: Bank): Unit = {
    // We cast the interface to the type that implements it
    this.bank = bank.asInstanceOf[b]
    this.balance = this.bank.a
  }
}