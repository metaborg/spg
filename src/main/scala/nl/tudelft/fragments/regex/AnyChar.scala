package nl.tudelft.fragments.regex

// A regular expression that matches any character
case class AnyChar[T]() extends Regex[T] {
  def derive(c: T) =
    Epsilon()

  def acceptsEmptyString =
    false

  def isEmptyString =
    false

  override def toString =
    "."

  override def rejectsAll: Boolean =
    false
}
