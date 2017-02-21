package org.metaborg.spg.core.terms

import org.metaborg.spg.core._

case class Var(name: String) extends Pattern {
  override def vars: List[Var] =
    List(this)

  override def size: Int =
    1

  override def substitute(binding: TermBinding): Pattern =
    binding.getOrElse(this, this)

  override def substituteScope(binding: TermBinding): Pattern =
    binding.getOrElse(this, this)

  override def substituteSort(binding: SortBinding): Pattern =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name)) {
      (nameBinding, Var(nameBinding(name)))
    } else {
      val fresh = "x" + nameProvider.next
      (nameBinding + (name -> fresh), Var(fresh))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = {
    if (this == typ) {
      Some(termBinding)
    } else if (typ.contains(this)) {
      None
    } else {
      typ match {
        case t@Var(_) if termBinding.contains(t) =>
          unify(termBinding(t), termBinding)
        case _ =>
          if (termBinding.contains(this)) {
            termBinding(this).unify(typ, termBinding)
          } else {
            Some(termBinding + (this -> typ))
          }
      }
    }
  }

  override def contains(p: Pattern): Boolean =
    false

  override def toString: String =
    s"""Var("$name")"""

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    if (f(this)) {
      Some(this)
    } else {
      None
    }

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    f(this)

  override def apply(index: Int): Pattern = {
    throw new RuntimeException(s"No child at index $index in $this")
  }
}
