package org.metaborg.spg.solver

import org.metaborg.spg.resolution.Graph
import org.metaborg.spg.spoofax.Language
import org.metaborg.spg.spoofax.models.{Sort, SortAppl, SortVar}
import org.metaborg.spg.{Pattern, Program, SymbolicName, Var}

object Solver {
  /**
    * Solves the given constraint by rewriting it and returning a new program.
    * A constraint can be solved in zero, one, or multiple ways, so we return a
    * list of programs.
    *
    * @param constraint
    * @param program
    * @param language
    * @return
    */
  def rewrite(constraint: Constraint, program: Program)(implicit language: Language): List[Program] = constraint match {
    case CTrue() =>
      program
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (program.typeEnv.contains(n)) {
        program + CEqual(program.typeEnv(n), t)
      } else {
        program.addBinding(n -> t)
      }
    case CEqual(t1, t2) =>
      t1.unify(t2).map(program.substitute)
    case CInequal(t1, t2) if t1.unify(t2).isEmpty =>
      program
    case CResolve(n1, n2) =>
      val declarations = Graph(program.constraints).res(program.resolution)(n1)

      declarations.toList.flatMap {
        case (_, declaration) =>
          declaration.unify(n2).map(unifier =>
            program
              .substitute(unifier)
              .copy(resolution = program.resolution + (n1 -> declaration))
          )
      }
    case CAssoc(n@SymbolicName(_, _), s@Var(_)) if Graph(program.constraints).associated(n).nonEmpty =>
      Graph(program.constraints).associated(n).map(scope =>
        program.substitute(Map(s -> scope))
      )
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !program.subtypes.domain.contains(t1) && !program.subtypes.isSubtype(t2, t1) =>
      val closure = for (ty1 <- program.subtypes.subtypeOf(t1); ty2 <- program.subtypes.supertypeOf(t2))
        yield (ty1, ty2)

      program.copy(subtypes = program.subtypes ++ closure)
    case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && program.subtypes.isSubtype(t1, t2) =>
      program
    // TODO: We can only solve this if the graph is complete w.r.t. the scope
    case CDistinct(Declarations(scope, namespace)) if scope.vars.isEmpty =>
      val names = Graph(program.constraints).declarations(scope, namespace)
      val combis = for (List(a, b, _*) <- names.combinations(2).toList) yield (a, b)

      program.addInequalities(combis)
    case recurse@CGenRecurse(name, pattern, scopes, typ, sort) =>
      language.rules(name, sort).flatMap(rule => {
        val freshRule = rule.instantiate().freshen()
        val newState = program.apply(recurse, freshRule)

        mergeSorts(newState)(sort, freshRule.sort).map(newState =>
          mergePatterns(newState)(pattern, freshRule.pattern).flatMap(newState =>
            mergeTypes(newState)(typ, freshRule.typ).flatMap(newState =>
              mergeScopes(newState)(scopes, freshRule.scopes)
            )
          )
        )
      })
    case _ =>
      Nil
  }

  def mergeSorts(program: Program)(s1: Sort, s2: Sort)(implicit language: Language): Option[Program] = s1 match {
    case SortVar(_) =>
      s1.unify(s2).map(program.substituteSort)
    case SortAppl(_, children) =>
      Sort
        .injectionsClosure(language.signatures, s1).view
        .flatMap(_.unify(s2))
        .headOption
        .map(program.substituteSort)
  }

  // TODO: It is possible that the pattern is aliased, so we should be more specific
  def mergePatterns(program: Program)(p1: Pattern, p2: Pattern)(implicit language: Language): List[Program] = {
    rewrite(CEqual(p1, p2), program)
  }

  def mergeTypes(program: Program)(t1: Option[Pattern], t2: Option[Pattern])(implicit language: Language): List[Program] = (t1, t2) match {
    case (None, None) =>
      program
    case (Some(_), Some(_)) =>
      rewrite(CEqual(t1.get, t2.get), program)
    case _ =>
      Nil
  }

  def mergeScopes(program: Program)(ss1: List[Pattern], ss2: List[Pattern])(implicit language: Language): List[Program] = {
    if (ss1.length == ss2.length) {
      ss1.zip(ss2).foldLeftMap(program) {
        case (program, (s1, s2)) =>
          rewrite(CEqual(s1, s2), program)
      }
    } else {
      Nil
    }
  }
}
