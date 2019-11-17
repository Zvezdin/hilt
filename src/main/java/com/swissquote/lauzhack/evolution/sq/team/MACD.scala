package com.swissquote.lauzhack.evolution.sq.team

class MACD(a: Indicator, b: Indicator) extends Indicator {
  override def price(c: BigDecimal): Option[BigDecimal] = {
    Some(a.last - b.last)
  }

  override def last: BigDecimal = a.last - b.last
}
