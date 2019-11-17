package com.swissquote.lauzhack.evolution.sq.team

class MACD(a: Indicator, b: Indicator) extends Indicator {
  override def price(c: BigDecimal): Option[BigDecimal] = {
    if( a.last - b.last == 0) return None
    Some(a.last - b.last)
  }

  override def last: BigDecimal = a.last - b.last
}
