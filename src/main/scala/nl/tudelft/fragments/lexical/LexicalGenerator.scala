package nl.tudelft.fragments.lexical

import nl.tudelft.fragments._
import nl.tudelft.fragments.spoofax.models._

import scala.util.Random

/**
  * A recursive-descent generator for a context-free grammar
  */
class LexicalGenerator(productions: List[Production]) {
  /**
    * Generate a string for the given symbol
    */
  def generate(symbol: Symbol): String = symbol match {
    // Recursively generate sort
    case Sort(_) =>
      val productionOpt = productions
        .filter(_.sort == symbol)
        .randomOption

      productionOpt
        .map(_.rhs.map(generate).mkString)
        .getOrElse(throw new IllegalStateException("No production for sort " + symbol))
    // Return literal text as-is
    case Lit(text) =>
      text
    // One or more repetitions
    case Iter(s) =>
      generate(s) + generate(IterStar(s))
    // Zero or more repetitions of s
    case IterStar(s) =>
      if (Coin.toss() == Coin.Head) {
        generate(s) + generate(symbol)
      } else {
        ""
      }
    // One or more repetitions of s (with separator)
    case IterSep(s, separator) =>
      if (Coin.toss() == Coin.Head) {
        generate(IterSep(s, separator)) + separator + generate(IterSep(s, separator))
      } else {
        generate(s)
      }
    // Zero or more repetitions of s (with separator)
    case IterStarSep(s, separator) =>
      if (Coin.toss() == Coin.Head) {
        generate(IterSep(s, separator))
      } else {
        ""
      }
    // Optionally generate symbol
    case Opt(s) =>
      if (Coin.toss() == Coin.Head) {
        generate(s)
      } else {
        ""
      }
    // Pick any character from a simple class
    case Simple(ranges @ _*) =>
      ranges
        .map(generate)
        .random
    // Pick any ASCII character (from the ASCII range 33-126) that is not in charClass
    case Comp(charClass) =>
      val ascii = 33 to 126
      val exclude = charClass.characters
      val difference = ascii diff exclude

      difference.random.toString
  }

  /**
    * Generate a string for the given character range
    */
  def generate(characterRange: CharacterRange): String = characterRange match {
    case Range(Short(start), Short(end)) =>
      (start to end).random.toString
    case Short(char) =>
      char.toString
  }
}

/**
  * Randomness
  */
object Coin extends Enumeration {
  type Coin = Value

  val Head, Tail = Value

  def toss(): Coin =
    if (Random.nextInt(2) == 0) {
      Head
    } else {
      Tail
    }
}
