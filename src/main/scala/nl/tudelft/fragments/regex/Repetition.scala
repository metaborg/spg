package nl.tudelft.fragments.regex

// A regular expression that matches n repetitions of the given regex.
case class Repetition[T](regex: Regex[T], n: Int) extends Regex[T] {
  def derive(c: T): Regex[T] =
    if (n < 0)
      EmptySet()
    else if (n == 0)
      Epsilon()
    else
      regex.derive(c) ~ (regex ^ (n - 1))

  def acceptsEmptyString =
    (n == 0) || ((n > 0) && regex.acceptsEmptyString)

  def isEmptyString =
    (n == 0) || ((n > 0) && regex.isEmptyString)

  override def rejectsAll: Boolean =
    n < 0 || regex.rejectsAll
}
