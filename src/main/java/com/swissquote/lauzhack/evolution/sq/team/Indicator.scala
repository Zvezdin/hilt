package com.swissquote.lauzhack.evolution.sq.team

trait Indicator {
  def price(c: BigDecimal): BigDecimal
  def last: BigDecimal
}
