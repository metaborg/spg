package org.metaborg.spg.core.regex

// A regular expression that matches two regular expressions in sequence.
case class Concatenation[T](prefix: Regex[T], suffix: Regex[T]) extends Regex[T] {
  def derive(c: T): Regex[T] =
    if (prefix.acceptsEmptyString)
      (prefix.derive(c) ~ suffix) || suffix.derive(c)
    else
      prefix.derive(c) ~ suffix

  def acceptsEmptyString =
    prefix.acceptsEmptyString && suffix.acceptsEmptyString

  def isEmptyString =
    prefix.isEmptyString && suffix.isEmptyString

  override def rejectsAll: Boolean =
    prefix.rejectsAll || suffix.rejectsAll
}
