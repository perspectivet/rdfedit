package com.github.perspectivet

import java.lang.{Iterable => JIterable}

object JavaUtils {
  def jforeach[T](iterable:JIterable[T],f:T => Unit) {
    val it = iterable.iterator
    while(it.hasNext) {
      f(it.next)
    }
  }
}
