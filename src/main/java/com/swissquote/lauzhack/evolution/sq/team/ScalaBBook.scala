package com.swissquote.lauzhack.evolution.sq.team;

import java.math.BigDecimal

import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Currency, Price, Trade}
import org.ta4j.core.{Bar, BarSeries, BaseBarBuilder, BaseBarSeriesBuilder}
import org.ta4j.core.aggregator.BaseBarSeriesAggregator
import org.ta4j.core.num.Num

import scala.collection.mutable

object TeA {
  def getIndicators = List(new SMA(4), new SMA(4), new SMA(4))


}

class ScalaBBook extends BBook {
  implicit def long2decimal(num: Int): BigDecimal = new BigDecimal(num)
  private var bank: Bank = _
  private var charts = new mutable.HashMap[(Currency, Currency), List[Indicator]]()

  charts((Currency.EUR, Currency.CHF)) = TeA.getIndicators
  charts((Currency.JPY, Currency.CHF)) = TeA.getIndicators
  charts((Currency.USD, Currency.CHF)) = TeA.getIndicators
  charts((Currency.GBP, Currency.CHF)) = TeA.getIndicators


  override def onInit(): Unit = {
    // Start by buying some cash. Don't search for more logic here: numbers are just random..
    bank.buy(new Trade(Currency.EUR, Currency.CHF, new BigDecimal(100000)))
    bank.buy(new Trade(Currency.JPY, Currency.CHF, new BigDecimal(1000000)))
    bank.buy(new Trade(Currency.USD, Currency.CHF, new BigDecimal(100000)))
    bank.buy(new Trade(Currency.GBP, Currency.CHF, new BigDecimal(100000)))

  }
  
  override def onTrade(trade: Trade): Unit = {

    if (Math.random < 0.05) {
      val coverTrade = new Trade(trade.base, trade.term, trade.quantity multiply 2)
      bank.buy(coverTrade)
    }
  }

  override def onPrice(price: Price): Unit = {
    val series = charts((price.base, price.term))
    println(series map { _.price(price.rate) })

  }

  override def setBank(bank: Bank): Unit = {
    this.bank = bank;
  }
}