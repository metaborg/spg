package org.metaborg.spg.core.terms

import org.metaborg.spg.core._
import org.metaborg.spg.core.spoofax.models.Strategy

abstract class Pattern {
  def vars: List[Var]

  def size: Int

  def substitute(binding: TermBinding): Pattern

  def substituteScope(binding: TermBinding): Pattern

  def substituteType(binding: TermBinding): Pattern =
    substituteScope(binding)

  def substituteSort(binding: SortBinding): Pattern

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern)

  def unify(t: Pattern, termBinding: TermBinding = Map.empty): Option[TermBinding]

  def rewrite(s: Strategy): Pattern =
    s(this).get

  /**
    * Check if the given pattern is contained in the current pattern.
    *
    * When unifying a variable with another term, we need to make sure the
    * variable does not occur in the other term.
    *
    * @param pattern
    * @return
    */
  def contains(pattern: Pattern): Boolean

  def find(f: Pattern => Boolean): Option[Pattern]

  def apply(index: Int): Pattern

  /**
    * Apply f at every node in the tree and collect all results f(x).
    *
    * @param f
    * @return
    */
  def collect(f: Pattern => List[Pattern]): List[Pattern]
}
