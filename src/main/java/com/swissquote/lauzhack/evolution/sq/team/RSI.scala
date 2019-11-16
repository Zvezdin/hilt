package com.swissquote.lauzhack.evolution.sq.team

class RSI extends Indicator {
  var prevC: BigDecimal = BigDecimal(0)
  var gain: BigDecimal = BigDecimal(0)
  var loss: BigDecimal = BigDecimal(0)

  override def price(c: BigDecimal): BigDecimal = {
    if( (prevC - c) < 0 ) {
      loss = (loss * (prevC - c)) / 2
    } else {
      gain = (gain * (prevC - c)) / 2
    }

    100 - (100/ 1 + (gain / loss))
  }

  override def last: BigDecimal = 100 - (100/ 1 + (gain / loss))
}
