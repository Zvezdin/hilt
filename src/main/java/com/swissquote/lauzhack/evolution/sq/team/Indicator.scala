package com.swissquote.lauzhack.evolution.sq.team

trait Indicator {
  def price(c: BigDecimal): Option[BigDecimal]
  def last: BigDecimal
}
