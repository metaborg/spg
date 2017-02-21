package org.metaborg.spg.core.regex

// A regular expression that matches a set of characters.
case class CharSet[T](set: Set[T]) extends Regex[T] {
  def derive(c: T) =
    if (set contains c)
      Epsilon()
    else
      EmptySet()

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    NotCharSet(set)

  override def rejectsAll: Boolean =
    set.isEmpty
}
