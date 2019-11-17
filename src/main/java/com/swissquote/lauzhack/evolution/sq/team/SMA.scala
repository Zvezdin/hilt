package com.swissquote.lauzhack.evolution.sq.team

import org.apache.commons.collections4.queue.CircularFifoQueue

class SMA(count: Int) extends Indicator {
  var buffer = new CircularFifoQueue[BigDecimal](count)
  var sum = BigDecimal(0);
  def price(c: BigDecimal): Option[BigDecimal] = {
    val prev = buffer.peek()
    if(buffer.isAtFullCapacity){
      sum += (c - prev) / BigDecimal(count)
      buffer.add(c)
    }else{
      sum += c / BigDecimal(count)
      buffer.add(c)
      return None
    }
    Some(sum)
  }

  override def last: BigDecimal = sum
}
