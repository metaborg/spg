package nl.tudelft.fragments.regex

// Kleene star of the given regex.
case class Star(regex: Regex) extends Regex {
  def derive(c: Char): Regex =
    regex.derive(c) ~ (regex *)

  def acceptsEmptyString =
    true

  def isEmptyString =
    regex.isEmptyString || regex.isEmptyString

  override def rejectsAll: Boolean =
    false
}
