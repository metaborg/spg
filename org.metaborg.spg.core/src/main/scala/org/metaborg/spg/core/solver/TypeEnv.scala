package org.metaborg.spg.core.solver

import org.metaborg.spg.core.terms.{Pattern, Var}
import org.metaborg.spg.core._
import org.metaborg.spg.core.stratego.Strategy

/**
  * Representation of a typing environment
  *
  * @param bindings Bindings from names to types
  */
case class TypeEnv(bindings: Map[Pattern, Pattern] = Map.empty) {
  def contains(n: Pattern): Boolean =
    bindings.contains(n)

  def apply(n: Pattern) =
    bindings(n)

  def +(e: (Pattern, Pattern)) =
    TypeEnv(bindings + e)

  def ++(typeEnv: TypeEnv) =
    TypeEnv(bindings ++ typeEnv.bindings)

  /**
    * Merge the given type environment with this type environment.
    *
    * The merge combines declarations from both environments. If a
    * declaration occurs in both environments, the associated types are
    * unified.
    *
    * @param that
    * @return
    */
  def merge(that: TypeEnv): Option[(TypeEnv, TermBinding)] = {
    // Bindings that both environments have in common
    val both = bindings.filterKeys(name =>
      that.bindings.contains(name)
    ).map {
      case (k, v1) =>
        (v1, that.bindings(k))
    }

    // Unify types of bindings with the same name in both environments
    val unifierOpt = both.foldLeft(Option(Map.empty[Var, Pattern])) {
      case (None, _) =>
        None
      case (Some(unifier), (t1, t2)) =>
        t1.unify(t2, unifier)
    }

    unifierOpt.map(unifier =>
      (TypeEnv(bindings ++ that.bindings).substitute(unifier), unifier)
    )
  }

  def substitute(termBinding: TermBinding): TypeEnv =
    TypeEnv(
      bindings.map { case (name, typ) =>
        name -> typ.substitute(termBinding)
      }
    )

  def substituteScope(termBinding: TermBinding): TypeEnv =
    substitute(termBinding)

  def freshen(nameBinding: Map[String, String]): (Map[String, String], TypeEnv) = {
    val freshBindings = bindings.toList.mapFoldLeft(nameBinding) { case (nameBinding, (name, typ)) =>
      name.freshen(nameBinding).map { case (nameBinding, name) =>
        typ.freshen(nameBinding).map { case (nameBinding, typ) =>
          (nameBinding, name -> typ)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, TypeEnv(bindings.toMap))
    }
  }

  def rewrite(strategy: Strategy) = {
    TypeEnv(
      bindings.map { case (name, typ) =>
        name.rewrite(strategy) -> typ.rewrite(strategy)
      }
    )
  }

  override def toString =
    "TypeEnv(Map(" + bindings.map { case (name, typ) => s"""Binding($name, $typ)""" }.mkString(", ") + "))"
}
