package nl.tudelft.fragments

// Based on a post by Matthew Might (http://goo.gl/WZmLaj)

abstract class Regex {
  def mustBeSubsumedBy(re2: Regex): Boolean = {
    (this, re2) match {
      case (Character(c1),Character(c2)) =>
        c1 == c2
      case (Character(c1),`AnyChar`) =>
        true
      case _ =>
        false
    }
  }

  // Kleene star
  def * : Regex =
    if (this.isEmptyString)
      this
    else if (this.rejectsAll)
      Epsilon
    else
      Star(this)

  // n repetitions
  def ^(n: Int): Regex =
    if (n < 0)
      EmptySet
    else if (n == 0)
      Epsilon
    else if (n == 1)
      this
    else if (this.isEmptyString)
      Epsilon
    else if (this.rejectsAll)
      EmptySet
    else
      Repetition(this, n)

  // Kleene plus
  def `+`: Regex =
    if (this.isEmptyString)
      this
    else if (this.rejectsAll)
      EmptySet
    else
      this ~ Star(this)

  def `?`: Regex =
    if (this.isEmptyString)
      this
    else if (this.rejectsAll)
      Epsilon
    else
      Epsilon || this

  def ~(suffix: Regex): Regex =
    if (this.isEmptyString)
      suffix
    else if (suffix.isEmptyString)
      this
    else if (this.rejectsAll)
      EmptySet
    else if (suffix.rejectsAll)
      EmptySet
    else
      Concatenation(this, suffix)

  def ||(that: Regex): Regex =
    if (this.rejectsAll)
      that
    else if (that.rejectsAll)
      this
    else if (this.mustBeSubsumedBy(that))
      that
    else if (that.mustBeSubsumedBy(this))
      this
    else
      Union(this, that)

  // Derivative of this regular expression with respect to given character
  def derive(c: Char): Regex

  // Derivative of this regular expression with respect to the end of input
  def deriveEND: Regex =
    EmptySet

  // True iff the regular expression accepts the empty string.
  def acceptsEmptyString: Boolean

  // True if the regular expression accepts no strings at all.
  def rejectsAll: Boolean

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

  def rejectsAll: Boolean =
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

  def rejectsAll: Boolean =
    set.isEmpty

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

  // NOTE: If the set size is the same as the
  // number of unicode characters, it is the empty set:
  def rejectsAll: Boolean =
    set.size == 100713

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

  def rejectsAll =
    prefix.rejectsAll || suffix.rejectsAll

  def isEmptyString =
    prefix.isEmptyString && suffix.isEmptyString
}

// A regex that matches either choice1 or choice2
case class Union(choice1: Regex, choice2: Regex) extends Regex {
  def derive(c: Char): Regex =
    choice1.derive(c) || choice2.derive(c)

  def acceptsEmptyString =
    choice1.acceptsEmptyString || choice2.acceptsEmptyString

  def rejectsAll =
    choice1.rejectsAll && choice2.rejectsAll

  def isEmptyString =
    choice1.isEmptyString && choice2.isEmptyString
}

// Kleene star of the given regex
case class Star(regex: Regex) extends Regex {
  def derive(c: Char): Regex =
    regex.derive(c) ~ (regex *)

  def acceptsEmptyString =
    true

  def rejectsAll =
    false

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

  def rejectsAll =
    (n < 0) || regex.rejectsAll

  def isEmptyString =
    (n == 0) || ((n > 0) && regex.isEmptyString)
}

case object END extends Regex {
  def derive(c: Char) =
    EmptySet

  override def deriveEND =
    Epsilon

  def acceptsEmptyString =
    false

  def rejectsAll =
    false

  def isEmptyString =
    false

  override def toString =
    "$$$"
}

case object EmptySet extends Regex {
  def derive(c: Char) =
    this

  def acceptsEmptyString =
    false

  def rejectsAll =
    true

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

  def rejectsAll =
    false

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

  def rejectsAll =
    false

  def isEmptyString =
    false

  override def toString =
    "."
}
