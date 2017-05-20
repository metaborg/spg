package org.metaborg.spg.core

import org.metaborg.spg.core.terms.{Pattern, Var}

import scala.collection.immutable.Map

/**
  * A unifier is a map from variables to patterns.
  *
  * @param delegate
  */
case class Unifier(delegate: Map[Var, Pattern]) {
  /**
    * Create a new unifier that includes the given binding.
    *
    * @param kv
    * @return
    */
  def +(kv: (Var, Pattern)): Unifier = {
    Unifier(delegate + kv)
  }

  /**
    * Compose unifiers.
    *
    * Composition is an associative operation and is compatible with
    * substitution application, i.e. (γ ◦ σ) t = γ (σ t). However, composition
    * of unifiers is not commutative: σ ◦ γ may be different from γ ◦ σ.
    *
    * @param unifier
    * @return
    */
  def ++(unifier: Unifier): Unifier = {
    // The unifiers may not have overlapping keys (e.g. X |-> a, X |-> b)
    assert(delegate.keySet intersect unifier.delegate.keySet isEmpty)

    val updatedMap = unifier.delegate.map {
      case (variable, pattern) =>
        variable -> pattern.substitute(delegate)
    }

    Unifier(updatedMap ++ delegate)
  }
}

object Unifier {
  /**
    * Factory method for creating an empty unifier.
    *
    * @return
    */
  def empty: Unifier = {
    new Unifier(Map.empty)
  }
}
