package org.metaborg.spg.regex

// Based on a post by Matthew Might (http://goo.gl/WZmLaj). The methods in `Regex` normalize the result to prevent
// the data structure from exploding when performing repetitive derivative calculations. Also, regular expressions
// have been generalized to an arbitrary alphabet of type T.

// Regular expressions over the alphabet described by type T.
abstract class Regex[T] {
  def `*`: Regex[T] =
    Star(this)

  def ^(n: Int): Regex[T] =
    Repetition(this, n)

  def `+`: Regex[T] =
    this ~ Star(this)

  def `?`: Regex[T] =
    Epsilon() || this

  def ~(that: Regex[T]): Regex[T] =
    if (this.isEmptyString) {
      that
    } else if (that.isEmptyString) {
      this
    } else if (this.rejectsAll || that.rejectsAll) {
      EmptySet()
    } else {
      Concatenation(this, that)
    }

  def &(that: Regex[T]): Regex[T] =
    Intersection(this, that)

  def ||(that: Regex[T]): Regex[T] =
    if (this.rejectsAll) {
      that
    } else if (that.rejectsAll) {
      this
    } else {
      Union(this, that)
    }

  // Derivative of this regular expression with respect to given character sequence
  def derive(ts: Seq[T]): Regex[T] =
    ts.foldLeft(this)((re, t) => re.derive(t))

  // Derivative of this regular expression with respect to given character
  def derive(c: T): Regex[T]

  // True iff the regular expression accepts the empty string.
  def acceptsEmptyString: Boolean

  // True iff this regular expression accepts only the empty string.
  def isEmptyString: Boolean

  // True iff this regular expression describes the empty language.
  def rejectsAll: Boolean
}
