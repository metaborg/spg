package org.metaborg.spg.core.spoofax.models

import org.metaborg.spg.core.terms.{As, Pattern, TermAppl}

abstract class Strategy extends (Pattern => Option[Pattern])

object Strategy {
  val id: Strategy = new Strategy {
    override def apply(p: Pattern) =
      Some(p)
  }

  val fail: Strategy = new Strategy {
    override def apply(p: Pattern) =
      None
  }

  val attempt: (Strategy => Strategy) = (s: Strategy) => new Strategy {
    override def apply(p: Pattern) =
      s(p) orElse Some(p)
  }

  val all: (Strategy => Strategy) = (s: Strategy) => new Strategy {
    override def apply(p: Pattern) = p match {
      case TermAppl(cons, children) =>
        val childrenOpt = children.foldRight(Option.apply[List[Pattern]](Nil)) {
          case (child, None) =>
            Option.empty[List[Pattern]]
          case (child, Some(list)) =>
            s(child).map(_ :: list)
        }

        childrenOpt.map(children => TermAppl(cons, children))
      case As(_, term) =>
        all(s)(term)
      case _ =>
        Some(p)
    }
  }

  val topdown: (Strategy => Strategy) = (s: Strategy) => new Strategy {
    override def apply(p: Pattern) =
      s(p).flatMap(p => all(topdown(s))(p))
  }

  def rewrite(s: => Strategy)(t: Pattern): Pattern =
    s(t).getOrElse(t)
}
