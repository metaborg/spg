package nl.tudelft.fragments.regex

// A regular expression that matches the empty string
case class Epsilon[T]() extends Regex[T] {
  def derive(c: T) =
    EmptySet()

  def acceptsEmptyString =
    true

  def isEmptyString =
    true

  override def toString =
    "e"

  override def rejectsAll: Boolean =
    false
}
