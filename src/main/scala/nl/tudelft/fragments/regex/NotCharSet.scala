package nl.tudelft.fragments.regex

// A regular expression that matches anything not in a set of characters.
case class NotCharSet(set: Set[Char]) extends Regex {
  def derive(a: Char) =
    if (set contains a)
      EmptySet
    else
      Epsilon

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    CharSet(set)

  override def rejectsAll: Boolean =
    false
}
