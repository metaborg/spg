package org.metaborg.spg.core.terms

import org.metaborg.spg.core.{_}

case class As(alias: Var, term: Pattern) extends Term {
  override def vars: List[Var] =
    term.vars

  override def size: Int =
    term.size

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    if (binding.contains(alias)) {
      binding(alias)
    } else {
      As(alias, term.substitute(binding))
    }

  override def substituteScope(binding: TermBinding): Pattern =
    As(alias, term.substituteScope(binding))

  override def substituteSort(binding: SortBinding): Pattern =
    As(alias, term.substituteSort(binding))

  override def freshen(binding: Map[String, String]): (Map[String, String], Pattern) =
    alias.freshen(binding).map { case (binding, alias) =>
      term.freshen(binding).map { case (binding, term) =>
        (binding, As(alias.asInstanceOf[Var], term))
      }
    }

  override def unify(t: Pattern, termBinding: TermBinding): Option[TermBinding] = t match {
    case o@As(_, _) =>
      term.unify(o.term, termBinding).flatMap(termBinding =>
        o.alias.unify(o.alias, termBinding)
      )
    case TermAppl(_, _) =>
      t.unify(term, termBinding)
    case Var(_) =>
      t.unify(this, termBinding)
    case _ =>
      None
  }

  override def contains(p: Pattern): Boolean =
    term.contains(p)

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    ???

  override def collect(f: (Pattern) => List[Pattern]): List[Pattern] =
    f(this) ++ term.collect(f)

  override def apply(index: Int): Pattern = {
    term(index)
  }
}
