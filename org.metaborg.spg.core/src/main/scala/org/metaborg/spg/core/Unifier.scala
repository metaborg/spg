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
    * Combine this unifier with the given unifier.
    *
    * @param unifier
    * @return
    */
  def ++(unifier: Unifier): Unifier = {
    assert((delegate.keySet intersect unifier.delegate.keySet).isEmpty)

    Unifier(delegate ++ unifier.delegate)
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
