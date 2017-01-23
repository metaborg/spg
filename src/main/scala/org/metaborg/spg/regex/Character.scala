package org.metaborg.spg.regex

// A regular expression that matches a specific value of type T.
case class Character[T](c: T) extends Regex[T] {
  def derive(a: T) =
    if (c == a)
      Epsilon()
    else
      EmptySet()

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  override def toString =
    "'" + c + "'"

  override def rejectsAll: Boolean =
    false
}
