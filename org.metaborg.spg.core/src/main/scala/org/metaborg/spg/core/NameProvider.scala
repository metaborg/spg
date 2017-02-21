package org.metaborg.spg.core

// Name provider
case class NameProvider(i: Int) {
  var c = i

  def next = {
    c = c + 1
    c
  }

  def reset() =
    c = i
}
