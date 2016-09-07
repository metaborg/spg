package nl.tudelft.fragments.regex

// Based on a post by Matthew Might (http://goo.gl/WZmLaj). The methods in `Regex` normalize the result to prevent
// the data structure from exploding when performing repetitive derivative calculations.

abstract class Regex {
  def `*`: Regex =
    Star(this)

  def ^(n: Int): Regex =
    Repetition(this, n)

  def `+`: Regex =
    this ~ Star(this)

  def `?`: Regex =
    Epsilon || this

  def ~(that: Regex): Regex =
    if (this.isEmptyString) {
      that
    } else if (that.isEmptyString) {
      this
    } else if (this.rejectsAll || that.rejectsAll) {
      EmptySet
    } else {
      Concatenation(this, that)
    }

  def &(that: Regex): Regex =
    Intersection(this, that)

  def ||(that: Regex): Regex =
    if (this.rejectsAll) {
      that
    } else if (that.rejectsAll) {
      this
    } else {
      Union(this, that)
    }

  // Derivative of this regular expression with respect to given character sequence
  def derive(cs: String): Regex =
    cs.foldLeft(this)((regex, char) => regex.derive(char))

  // Derivative of this regular expression with respect to given character
  def derive(c: Char): Regex

  // True iff the regular expression accepts the empty string.
  def acceptsEmptyString: Boolean

  // True iff this regular expression accepts only the empty string.
  def isEmptyString: Boolean

  // True iff this regular expression describes the empty language.
  def rejectsAll: Boolean
}
