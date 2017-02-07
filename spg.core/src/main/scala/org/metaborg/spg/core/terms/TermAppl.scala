package org.metaborg.spg.core.terms

import org.metaborg.spg.core._

/**
  * A constructor application
  *
  * @param cons
  * @param children
  */
case class TermAppl(cons: String, children: List[Pattern] = Nil) extends Term {
  override def vars: List[Var] =
    children.flatMap(_.vars).distinct

  override def size: Int =
    1 + children.map(_.size).sum

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substitute(binding)))

  override def substituteScope(binding: TermBinding): Pattern =
    TermAppl(cons, children.map(_.substituteScope(binding)))

  override def substituteSort(binding: SortBinding): Pattern =
    TermAppl(cons, children.map(_.substituteSort(binding)))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = {
    typ match {
      case c@TermAppl(`cons`, _) if children.length == c.children.length =>
        children.zip(c.children).foldLeftWhile(termBinding) {
          case (termBinding, (t1, t2)) =>
            t1.unify(t2, termBinding)
        }
      case Var(_) =>
        typ.unify(this, termBinding)
      case _ =>
        None
    }
  }

  override def contains(p: Pattern): Boolean =
    children.exists(child => child == p || child.contains(p))

  override def toString: String =
    s"""TermAppl("$cons", $children)"""

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    if (f(this)) {
      Some(this)
    } else {
      for (child <- children) {
        child.find(f) match {
          case x@Some(_) => return x
          case _ =>
        }
      }

      None
    }

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    f(this) ++ children.flatMap(_.collect(f))

  def arity: Int =
    children.length

  override def apply(index: Int): Pattern = {
    children(index)
  }
}
