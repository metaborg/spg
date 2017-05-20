package org.metaborg.spg.core

import org.metaborg.spg.core.terms.Pattern

object Unifier {
  /**
    * Unify two patterns.
    *
    * TODO: We might want to move the unification out of the term library, and
    * into this Unifier class. We can still make these method implicitly
    * available on the term library.
    *
    * @param p1
    * @param p2
    * @return
    */
  def unify(p1: Pattern, p2: Pattern): Option[Substitution] = {
    p1.unify(p2).map(Substitution.apply)
  }

  /**
    * Unify two lists of patterns.
    *
    * @param l1
    * @param l2
    * @return
    */
  def unify(l1: List[Pattern], l2: List[Pattern]): Option[Substitution] = {
    if (l1.length != l2.length) {
      return None
    }

    l1.zip(l2).foldLeftWhile(Substitution.empty) { case (unifier, (p1, p2)) =>
      val q1 = p1.substitute(unifier)
      val q2 = p2.substitute(unifier)

      unify(q1, q2).map(newUnifier =>
        newUnifier ++ unifier
      )
    }
  }
}
