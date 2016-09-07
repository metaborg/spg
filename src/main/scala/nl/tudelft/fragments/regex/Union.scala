package nl.tudelft.fragments.regex

// A regex that matches either choice1 or choice2.
case class Union[T](choice1: Regex[T], choice2: Regex[T]) extends Regex[T] {
  def derive(c: T): Regex[T] =
    choice1.derive(c) || choice2.derive(c)

  def acceptsEmptyString =
    choice1.acceptsEmptyString || choice2.acceptsEmptyString

  def isEmptyString =
    choice1.isEmptyString && choice2.isEmptyString

  override def rejectsAll: Boolean =
    choice1.rejectsAll && choice2.rejectsAll
}
