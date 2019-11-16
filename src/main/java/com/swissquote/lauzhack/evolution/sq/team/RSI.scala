package com.swissquote.lauzhack.evolution.sq.team

class RSI extends Indicator {
  var prevC: BigDecimal = BigDecimal(0)
  var gain = new SMA(4)
  var loss = new SMA(4)

  override def price(c: BigDecimal): BigDecimal = {
    if( (prevC - c) < 0 ) {
      loss.price((prevC - c))
    } else {
      gain.price((prevC - c))
    }
    prevC = c
    if(loss.last == 0 || gain.last == 0) return BigDecimal(0)
    BigDecimal(100) - ( BigDecimal(100) / (BigDecimal(1) + (gain.last / loss.last)) )
  }

  override def last: BigDecimal = 100 - (100/ 1 + (gain.last / loss.last))
}
