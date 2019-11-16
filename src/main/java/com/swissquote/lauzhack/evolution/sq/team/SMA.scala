package com.swissquote.lauzhack.evolution.sq.team

import org.apache.commons.collections4.queue.CircularFifoQueue

class SMA(count: Int) extends Indicator {
  var buffer = new CircularFifoQueue[BigDecimal](count)
  var sum = BigDecimal(0)

  def price(c: BigDecimal): BigDecimal = {
    if(buffer.isFull){
      var prev = buffer.peek()
      sum = sum +  (c - prev) * (1/count)
    }

    buffer.add(c)
    sum
  }
}
