package com.swissquote.lauzhack.evolution.sq.team

import org.apache.commons.collections4.queue.CircularFifoQueue



class SMA(count: Int) extends Indicator {
  var buffer = new CircularFifoQueue[BigDecimal](count)
  var sum = BigDecimal(0);
  def price(c: BigDecimal): BigDecimal = {
    if(buffer.isAtFullCapacity){
      var prev = buffer.peek()
      sum = sum + (c - prev) * (BigDecimal(1) / BigDecimal(count))
    }
    buffer.add(c)
    sum
  }

  override def last: BigDecimal = sum
}
