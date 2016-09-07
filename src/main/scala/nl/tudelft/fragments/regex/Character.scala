package nl.tudelft.fragments.regex

// A regular expression that matches a specific character.
case class Character(c: Char) extends Regex {
  def derive(a: Char) =
    if (c == a)
      Epsilon
    else
      EmptySet

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  override def toString =
    "'" + c + "'"

  override def rejectsAll: Boolean =
    false
}
