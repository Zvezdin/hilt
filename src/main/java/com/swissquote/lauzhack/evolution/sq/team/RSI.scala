package com.swissquote.lauzhack.evolution.sq.team

class RSI extends Indicator {
  var prevC: BigDecimal = BigDecimal(0)
  var gain = new SMA(4)
  var loss = new SMA(4)

  override def price(c: BigDecimal): Option[BigDecimal] = {
    if( prevC == c) {
      return None
    }
    if( (prevC - c) < 0 ) {
      loss.price((c - prevC)).isEmpty
    } else {
      gain.price((prevC - c)).isEmpty
    }
    prevC = c
    if(!gain.buffer.isAtFullCapacity || !loss.buffer.isAtFullCapacity) return None
    Some(BigDecimal(100) - ( BigDecimal(100) / (BigDecimal(1) + (gain.last / loss.last)) ))
  }

  override def last: BigDecimal = 100 - (100/ 1 + (gain.last / loss.last))
}
