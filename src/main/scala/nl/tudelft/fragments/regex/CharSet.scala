package nl.tudelft.fragments.regex

// A regular expression that matches a set of characters.
case class CharSet(set: Set[Char]) extends Regex {
  def derive(a: Char) =
    if (set contains a)
      Epsilon
    else
      EmptySet

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    NotCharSet(set)

  override def rejectsAll: Boolean =
    set.isEmpty
}
