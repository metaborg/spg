package nl.tudelft.fragments

import scala.util.Random

case class Distribution[T](elems: List[(T, Int)]) {
  /**
    * Sum of weights.
    *
    * @return
    */
  lazy val total = elems.map(_._2).sum

  /**
    * Sample an element from this distribution.
    *
    * @return
    */
  def sample: T = {
    assert(total > 0, s"Total weight must be positive, was negative in $elems")

    val randomInt = Random.nextInt(total)
    var p = 0

    for ((elem, weight) <- elems) {
      if (p + weight >= randomInt) {
        return elem
      }

      p = p + weight
    }

    throw new IllegalStateException("Unable to sample element")
  }
}
