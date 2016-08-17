package nl.tudelft.fragments

// Based on a post by Matthew Might (http://goo.gl/WZmLaj)

abstract class Regex {
  def `*`: Regex =
    Star(this)

  def ^(n: Int): Regex =
    Repetition(this, n)

  def `+`: Regex =
    this ~ Star(this)

  def `?`: Regex =
    Epsilon || this

  def ~(suffix: Regex): Regex =
    Concatenation(this, suffix)

  def &(that: Regex): Regex =
    Intersection(this, that)

  def ||(that: Regex): Regex =
    Union(this, that)

  // Derivative of this regular expression with respect to given character
  def derive(c: Char): Regex

  // True iff the regular expression accepts the empty string.
  def acceptsEmptyString: Boolean

  // True iff this regular expression accepts only the empty string.
  def isEmptyString: Boolean
}

// A regular expression that matches a specific character.
case class Character(c: Char) extends Regex {
  def derive(a: Char) =
    if (c == a)
      Epsilon
    else
      EmptySet

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  override def toString =
    "'" + c + "'"
}

// A regular expression that matches a set of characters.
case class CharSet(set: Set[Char]) extends Regex {
  def derive(a: Char) =
    if (set contains a)
      Epsilon
    else
      EmptySet

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    NotCharSet(set)
}

// A regular expression that matches anything not in a set of characters.
case class NotCharSet(set: Set[Char]) extends Regex {
  def derive(a: Char) =
    if (set contains a)
      EmptySet
    else
      Epsilon

  def acceptsEmptyString: Boolean =
    false

  def isEmptyString: Boolean =
    false

  def unary_! =
    CharSet(set)
}

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
}

// A regex that matches either choice1 or choice2
case class Union(choice1: Regex, choice2: Regex) extends Regex {
  def derive(c: Char): Regex =
    choice1.derive(c) || choice2.derive(c)

  def acceptsEmptyString =
    choice1.acceptsEmptyString || choice2.acceptsEmptyString

  def isEmptyString =
    choice1.isEmptyString && choice2.isEmptyString
}

// A regex that matches both choice1 and choice2
case class Intersection(choice1: Regex, choice2: Regex) extends Regex {
  def derive(c: Char): Regex =
    choice1.derive(c) & choice2.derive(c)

  def acceptsEmptyString =
    choice1.acceptsEmptyString && choice2.acceptsEmptyString

  def isEmptyString =
    choice1.isEmptyString || choice2.isEmptyString
}

// Kleene star of the given regex
case class Star(regex: Regex) extends Regex {
  def derive(c: Char): Regex =
    regex.derive(c) ~ (regex *)

  def acceptsEmptyString =
    true

  def isEmptyString =
    regex.isEmptyString || regex.isEmptyString
}

// A regex that matches n repetitions of the given regex
case class Repetition(regex: Regex, n: Int) extends Regex {
  def derive(c: Char): Regex =
    if (n <= 0)
      Epsilon
    else
      regex.derive(c) ~ (regex ^ (n - 1))

  def acceptsEmptyString =
    (n == 0) || ((n > 0) && regex.acceptsEmptyString)

  def isEmptyString =
    (n == 0) || ((n > 0) && regex.isEmptyString)
}

case object EmptySet extends Regex {
  def derive(c: Char) =
    this

  def acceptsEmptyString =
    false

  def isEmptyString =
    false

  override def toString =
    "{}"
}

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
}

// A regular expression that matches any character
case object AnyChar extends Regex {
  def derive(c: Char) =
    Epsilon

  def acceptsEmptyString =
    false

  def isEmptyString =
    false

  override def toString =
    "."
}
