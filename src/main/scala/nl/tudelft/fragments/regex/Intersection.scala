package nl.tudelft.fragments.regex

// A regex that matches both choice1 and choice2
case class Intersection(choice1: Regex, choice2: Regex) extends Regex {
  def derive(c: Char): Regex =
    choice1.derive(c) & choice2.derive(c)

  def acceptsEmptyString =
    choice1.acceptsEmptyString && choice2.acceptsEmptyString

  def isEmptyString =
    choice1.isEmptyString || choice2.isEmptyString

  override def rejectsAll: Boolean =
    false
}
