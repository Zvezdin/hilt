package com.swissquote.lauzhack.evolution.sq.team;

import java.math.BigDecimal

import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Currency, Price, Trade}

class ScalaBBook extends BBook {
  implicit def long2decimal(num: Int): BigDecimal = new BigDecimal(num)
  private var bank: Bank = _

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

  }

  override def setBank(bank: Bank): Unit = {
    this.bank = bank;
  }
}