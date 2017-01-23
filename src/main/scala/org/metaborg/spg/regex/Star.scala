package org.metaborg.spg.regex

// Kleene star of the given regex.
case class Star[T](regex: Regex[T]) extends Regex[T] {
  def derive(c: T): Regex[T] =
    regex.derive(c) ~ (regex *)

  def acceptsEmptyString =
    true

  def isEmptyString =
    regex.isEmptyString || regex.isEmptyString

  override def rejectsAll: Boolean =
    false
}
