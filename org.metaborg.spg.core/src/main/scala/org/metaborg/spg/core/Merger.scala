package org.metaborg.spg.core

import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.{Sort, SortAppl, SortVar}
import org.metaborg.spg.core.terms.Pattern

object Merger {
  /**
    * Merge two sorts.
    *
    * @param rule
    * @param s1
    * @param s2
    * @param language
    * @return
    */
  def mergeSorts(rule: Rule)(s1: Sort, s2: Sort)(implicit language: Language): Option[Rule] = s1 match {
    case SortVar(_) =>
      s1.unify(s2).map(rule.substituteSort)
    case SortAppl(_, _) =>
      Sort
        .injectionsClosure(language.signatures, s1).view
        .flatMap(_.unify(s2))
        .headOption
        .map(rule.substituteSort)
  }

  /**
    * Merge two patterns.
    *
    * TODO: It is possible that the pattern is aliased, so we should be more specific
    *
    * @param program
    * @param p1
    * @param p2
    * @param language
    * @return
    */
  def mergePatterns(program: Rule)(p1: Pattern, p2: Pattern)(implicit language: Language): Option[Rule] = {
    p1.unify(p2).map(program.substitute)
  }

  /**
    * Merge two types.
    *
    * @param program
    * @param t1
    * @param t2
    * @param language
    * @return
    */
  def mergeTypes(program: Rule)(t1: Option[Pattern], t2: Option[Pattern])(implicit language: Language): Option[Rule] = (t1, t2) match {
    case (None, None) =>
      Some(program)
    case (Some(_), Some(_)) =>
      t1.get.unify(t2.get).map(program.substitute)
    case _ =>
      None
  }

  /**
    * Merge two lists of scopes.
    *
    * @param program
    * @param ss1
    * @param ss2
    * @param language
    * @return
    */
  def mergeScopes(program: Rule)(ss1: List[Pattern], ss2: List[Pattern])(implicit language: Language): Option[Rule] = {
    if (ss1.length == ss2.length) {
      ss1.zip(ss2).foldLeftWhile(program) {
        case (program, (s1, s2)) =>
          s1.unify(s2).map(program.substitute)
      }
    } else {
      None
    }
  }
}
