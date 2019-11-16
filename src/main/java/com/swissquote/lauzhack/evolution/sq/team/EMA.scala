package com.swissquote.lauzhack.evolution.sq.team

class EMA(smoothing: Double, count: Int) extends Indicator {
  var prev: BigDecimal = BigDecimal(0)

  override def price(c: BigDecimal): BigDecimal = {
    prev = c * ( smoothing / ( 1 + count )) + prev * (1 - ( smoothing / ( 1 + count )))
    prev
  }

  override def last: BigDecimal = prev
}
