package org.metaborg.spg.core.terms

import org.metaborg.spg.core._

case class TermString(name: String) extends Term {
  override def vars: List[Var] =
    Nil

  override def size: Int =
    1

  override def unify(p: Pattern, termBinding: TermBinding): Option[TermBinding] = p match {
    case TermString(n) if n == name  =>
      Some(termBinding)
    case Var(_) =>
      p.unify(this, termBinding)
    case _ =>
      None
  }

  override def contains(p: Pattern): Boolean =
    false

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    this

  override def substituteScope(binding: TermBinding): Pattern =
    ???

  override def substituteSort(binding: SortBinding): Pattern =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    (nameBinding, this)

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    None

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    Nil

  override def toString =
    s"""TermString("$name")"""

  override def apply(index: Int): Pattern = {
    throw new RuntimeException(s"No child at index $index in $this")
  }
}
