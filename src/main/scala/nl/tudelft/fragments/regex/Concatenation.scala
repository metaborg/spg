package nl.tudelft.fragments.regex

// A regular expression that matches two regular expressions in sequence.
case class Concatenation(prefix: Regex, suffix: Regex) extends Regex {
  def derive(c: Char): Regex =
    if (prefix.acceptsEmptyString)
      (prefix.derive(c) ~ suffix) || suffix.derive(c)
    else
      prefix.derive(c) ~ suffix

  def acceptsEmptyString =
    prefix.acceptsEmptyString && suffix.acceptsEmptyString

  def isEmptyString =
    prefix.isEmptyString && suffix.isEmptyString

  override def rejectsAll: Boolean =
    prefix.rejectsAll || suffix.rejectsAll
}
