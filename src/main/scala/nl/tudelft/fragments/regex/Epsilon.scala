package nl.tudelft.fragments.regex

// A regular expression that matches the empty string
case object Epsilon extends Regex {
  def derive(c: Char) =
    EmptySet

  def acceptsEmptyString =
    true

  def isEmptyString =
    true

  override def toString =
    "e"

  override def rejectsAll: Boolean =
    false
}
