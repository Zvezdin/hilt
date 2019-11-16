package com.swissquote.lauzhack.evolution.sq.team;

import com.swissquote.lauzhack.evolution.api.{BBook, Bank, Price, Trade}

class ScalaBBook extends BBook {
  private var bank: Bank = _

  override def onInit(): Unit = {

  }

  override def onTrade(trade: Trade): Unit = {

  }

  override def onPrice(price: Price): Unit = {

  }

  override def setBank(bank: Bank): Unit = {
    this.bank = bank;
  }
}