package nl.tudelft.fragments.regex

// A regular expression that matches nothing.
case class EmptySet[T]() extends Regex[T] {
  def derive(c: T) =
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
