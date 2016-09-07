package nl.tudelft.fragments.regex

// A regular expression that matches any character
case object AnyChar extends Regex {
  def derive(c: Char) =
    Epsilon

  def acceptsEmptyString =
    false

  def isEmptyString =
    false

  override def toString =
    "."

  override def rejectsAll: Boolean =
    false
}
