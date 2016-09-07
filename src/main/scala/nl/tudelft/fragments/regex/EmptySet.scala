package nl.tudelft.fragments.regex

// A regular expression that matches nothing.
case object EmptySet extends Regex {
  def derive(c: Char) =
    this

  def acceptsEmptyString =
    false

  def isEmptyString =
    false

  override def toString =
    "{}"

  override def rejectsAll: Boolean =
    true
}
