package nl.tudelft.fragments.regex

// A regular expression that matches anything not in a set of characters.
case class NotCharSet[T](set: Set[T]) extends Regex[T] {
  def derive(c: T) =
    if (set contains c)
      EmptySet()
    else
      Epsilon()

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    CharSet(set)

  override def rejectsAll: Boolean =
    false
}
