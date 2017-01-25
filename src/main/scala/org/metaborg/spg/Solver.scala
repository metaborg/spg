package org.metaborg.spg

import org.metaborg.spg.spoofax.models.{SortAppl, SortVar}
import org.metaborg.spg.spoofax.Language
import org.metaborg.spg.spoofax.models.Sort

object Solver {
  /**
    * Solves the given constraint by rewriting it and returning a new state.
    * Since a constraint may be solvable in multiple ways, a list of states is
    * returned. If the constraint cannot be solved, an empty list is returned.
    *
    * @param constraint
    * @param state
    * @param language
    * @return
    */
  def rewrite(constraint: Constraint, state: State)(implicit language: Language): List[State] = constraint match {
    case CTrue() =>
      state
    case CTypeOf(n, t) if n.vars.isEmpty =>
      if (state.typeEnv.contains(n)) {
        state + CEqual(state.typeEnv(n), t)
      } else {
        state.addBinding(n -> t)
      }
    case CEqual(t1, t2) =>
      t1.unify(t2).map(state.substitute)
    case CInequal(t1, t2) if t1.unify(t2).isEmpty =>
      state
    case CResolve(n1, n2) =>
      val declarations = Graph(state.constraints).res(state.resolution)(n1)

      declarations.flatMap(declaration =>
        declaration.unify(n2).map(unifier =>
          state
            .substitute(unifier)
            .copy(resolution = state.resolution + (n1 -> declaration))
        )
      )
    case CAssoc(n@SymbolicName(_, _), s@Var(_)) if Graph(state.constraints).associated(n).nonEmpty =>
      Graph(state.constraints).associated(n).map(scope =>
        state.substitute(Map(s -> scope))
      )
    case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !state.subtypeRelation.domain.contains(t1) && !state.subtypeRelation.isSubtype(t2, t1) =>
      val closure = for (ty1 <- state.subtypeRelation.subtypeOf(t1); ty2 <- state.subtypeRelation.supertypeOf(t2))
        yield (ty1, ty2)

      state.copy(subtypeRelation = state.subtypeRelation ++ closure)
    case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && state.subtypeRelation.isSubtype(t1, t2) =>
      state
    // TODO: These inequalities are barely used;
    case CDistinct(Declarations(scope, namespace)) if scope.vars.isEmpty =>
      val names = Graph(state.constraints).declarations(scope, namespace)
      val combis = for (List(a, b, _*) <- names.combinations(2).toList) yield (a, b)

      state.addInequalities(combis)
    case constraint@CGenRecurse(name, _, scopes, typ, sort) =>
      language.rules(name, sort).flatMap(rule => {
        val (_, freshRule) = rule.instantiate().freshen()

        // TODO: Merging in the presence of aliases is harder (try Tiger)
        val newState = state.merge(constraint, freshRule.state)

        mergeSorts(newState)(sort, freshRule.sort).map(newState =>
          mergeTypes(newState)(typ, freshRule.typ).flatMap(newState =>
            mergeScopes(newState)(scopes, freshRule.scopes)
          )
        )
      })
    case _ =>
      Nil
  }

  def mergeSorts(state: State)(s1: Sort, s2: Sort)(implicit language: Language): Option[State] = s1 match {
    case SortVar(_) =>
      s1.unify(s2).map(state.substituteSort)
    case SortAppl(_, children) =>
      Sort
        .injectionsClosure(language.signatures, s1).view
        .flatMap(_.unify(s2))
        .headOption
        .map(state.substituteSort)
  }

  def mergeTypes(state: State)(t1: Option[Pattern], t2: Option[Pattern])(implicit language: Language): List[State] = (t1, t2) match {
    case (None, None) =>
      state
    case (Some(_), Some(_)) =>
      rewrite(CEqual(t1.get, t2.get), state)
    case _ =>
      Nil
  }

  def mergeScopes(state: State)(ss1: List[Pattern], ss2: List[Pattern])(implicit language: Language): List[State] = {
    if (ss1.length == ss2.length) {
      ss1.zip(ss2).foldLeftMap(state) {
        case (state, (s1, s2)) =>
          rewrite(CEqual(s1, s2), state)
      }
    } else {
      Nil
    }
  }
}
