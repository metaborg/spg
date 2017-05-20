package org.metaborg.spg.core

import org.metaborg.spg.core.terms.{Pattern, Var}
import scala.collection.immutable.Map

/**
  * A substitution is a map from variables to patterns.
  *
  * @param delegate
  */
case class Substitution(delegate: Map[Var, Pattern]) {
  /**
    * Create a new substitution that includes the given binding.
    *
    * @param kv
    * @return
    */
  def +(kv: (Var, Pattern)): Substitution = {
    Substitution(delegate + kv)
  }
  /**
    * Compose substitutions.
    *
    * Substitution composition is an associative operation and is compatible
    * with substitution application, i.e. (γ ◦ σ) t = γ (σ t). Substitution
    * composition is not commutative: σ ◦ γ may be different from γ ◦ σ.
    *
    * @param unifier
    * @return
    */
  def ++(unifier: Substitution): Substitution = {
    // The unifiers may not have overlapping keys (e.g. X |-> a, X |-> b)
    assert(delegate.keySet intersect unifier.delegate.keySet isEmpty)
    val updatedMap = unifier.delegate.map {
      case (variable, pattern) =>
        variable -> pattern.substitute(delegate)
    }
    Substitution(updatedMap ++ delegate)
  }
}
object Substitution {
  /**
    * Create an empty substitution.
    *
    * @return
    */
  def empty: Substitution = {
    new Substitution(Map.empty)
  }
}
