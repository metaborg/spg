package org.metaborg.spg

class Binding[A, B](val a: A, val b: B)

object Binding {
  def apply[A, B](a: A, b: B): Binding[A, B] =
    new Binding(a, b)
}
