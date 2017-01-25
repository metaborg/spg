package org.metaborg.spg.solver

import org.metaborg.spg.{Pattern, _}

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

  override def toString =
    "TypeEnv(Map(" + bindings.map { case (name, typ) => s"""Binding($name, $typ)""" }.mkString(", ") + "))"
}
