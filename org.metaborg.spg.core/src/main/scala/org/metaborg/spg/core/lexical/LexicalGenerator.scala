package org.metaborg.spg.core.lexical

import org.metaborg.spg.core._
import org.metaborg.spg.core.sdf._

import scala.util.Random

/**
  * A recursive-descent generator for a context-free grammar
  */
class LexicalGenerator(val grammar: Grammar)(implicit val random: Random) {
  /**
    * Generate a string for the given symbol
    */
  def generate(symbol: Symbol): String = symbol match {
    // Recursively generate sort
    case SortAppl(_, Nil) =>
      val productionOpt = grammar.productions
        .filter(_.sort == symbol)
        .filter(!_.isReject)
        .randomOption

      productionOpt
        .map(_.rhs.map(generate).mkString)
        .getOrElse(throw new IllegalStateException("No production for sort " + symbol))
    // Return literal text as-is
    case Lit(text) =>
      text
    // Return literal text as-is (TODO: Create variants with capitalization)
    case CiLit(text) =>
      text
    // One or more repetitions
    case Iter(s) =>
      generate(s) + generate(IterStar(s))
    // Zero or more repetitions of s
    case IterStar(s) =>
      if (Coin(random).toss() == Coin.Head) {
        generate(s) + generate(symbol)
      } else {
        ""
      }
    // One or more repetitions of s (with separator)
    case IterSep(s, separator) =>
      if (Coin(random).toss() == Coin.Head) {
        generate(IterSep(s, separator)) + separator + generate(IterSep(s, separator))
      } else {
        generate(s)
      }
    // Zero or more repetitions of s (with separator)
    case IterStarSep(s, separator) =>
      if (Coin(random).toss() == Coin.Head) {
        generate(IterSep(s, separator))
      } else {
        ""
      }
    // Optionally generate symbol
    case Opt(s) =>
      if (Coin(random).toss() == Coin.Head) {
        generate(s)
      } else {
        ""
      }
    // Pick any character from a simple class
    case Simple(ranges@_*) =>
      ranges
        .map(generate)
        .random
    // Pick any ASCII character (from the ASCII range 33-126) that is not in charClass
    case Comp(charClass) =>
      val ascii = 33 to 126
      val exclude = charClass.characters
      val difference = ascii diff exclude

      difference.random.toChar.toString
    // TODO: Generate based on the layout productions
    case Layout() =>
      " "
    // Pick either alternative
    case Alt(s1, s2) =>
      if (Coin(random).toss() == Coin.Head) {
        generate(s1)
      } else {
        generate(s2)
      }
    case Sequence(s, ss) =>
      generate(s) ++ ss.map(generate).mkString
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
}

case class Coin(random: Random) {
  def toss(): Coin.Value = {
    if (random.nextInt(2) == 0) {
      Coin.Head
    } else {
      Coin.Tail
    }
  }
}
