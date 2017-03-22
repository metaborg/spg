package org.metaborg.spg.core.solver

import org.metaborg.spg.core._
import org.metaborg.spg.core.resolution.{Graph, Occurrence}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.spoofax.models.{Sort, SortAppl, SortVar, Strategy}
import org.metaborg.spg.core.{NameProvider, Program}
import org.metaborg.spg.core.resolution.OccurrenceImplicits._
import org.metaborg.spg.core.spoofax.models.Strategy._
import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString, Var}

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

      declarations.toList.flatMap(declaration =>
        // TODO: Substitution: if n1 or n2 was a variable, the names should be made equal and this transformation should be applied to the program
        n2
          .unify(declaration)
          .map(program.substitute)
          .map(_.addResolution(n1 -> declaration))
          .map(mergeOccurrences(_)(n1, declaration))
      )
    case CAssoc(n, s@Var(_)) if Graph(program.constraints).associated(n).nonEmpty =>
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

      program//.addInequalities(combis)
    case recurse@CGenRecurse(name, _, _, _, sort, size) =>
      language.rules(name, sort).flatMap(rule => {
        program.apply(recurse, rule)
      })
    case _ =>
      Nil
  }

  /**
    * Solve all constraints.
    *
    * @param program
    * @return
    */
  def solveAll(program: Program)(implicit language: Language): List[Program] = {
    if (program.properConstraints.isEmpty) {
      return Some(program)
    }

    for (constraint <- program.properConstraints) {
      val result = rewrite(constraint, program - constraint)

      if (result.nonEmpty) {
        return result.flatMap(solveAll)
      }
    }

    Nil
  }

  /**
    * Solve all deterministic constraints in the given program.
    *
    * A deterministic constraint is one that does not involve a choice. In
    * essence, they only propagate knowledge. For example, a TypeOf constraint
    * can only be solved in one way, and if it can be solved, it should be
    * solved.
    *
    * @param program
    * @return
    */
  def solveFixpoint(program: Program)(implicit language: Language): Program = {
    /**
      * Rewrite a basic constraint.
      *
      * @param constraint
      * @param program
      * @return
      */
    def rewrite(constraint: Constraint, program: Program)(implicit language: Language): Option[Program] = constraint match {
      case CTrue() =>
        program
      case CEqual(t1, t2) =>
        t1.unify(t2).map(program.substitute)
      case CTypeOf(n, t) if n.vars.isEmpty =>
        if (program.typeEnv.contains(n)) {
          program + CEqual(program.typeEnv(n), t)
        } else {
          program.addBinding(n -> t)
        }
      case CAssoc(n, s@Var(_)) if Graph(program.constraints).associated(n).nonEmpty =>
        Graph(program.constraints).associated(n).map(scope =>
          program.substitute(Map(s -> scope))
        )
      case FSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && !program.subtypes.domain.contains(t1) && !program.subtypes.isSubtype(t2, t1) =>
        val closure = for (ty1 <- program.subtypes.subtypeOf(t1); ty2 <- program.subtypes.supertypeOf(t2))
          yield (ty1, ty2)

        program.copy(subtypes = program.subtypes ++ closure)
      case CSubtype(t1, t2) if (t1.vars ++ t2.vars).isEmpty && program.subtypes.isSubtype(t1, t2) =>
        program
      case _ =>
        None
    }

    /**
      * Solve a single constraint or return the program if none can be solved.
      *
      * @param program
      * @return
      */
    def solveAny(program: Program)(implicit language: Language): Program = {
      for (constraint <- program.constraints) {
        rewrite(constraint, program - constraint) match {
          case Some(program) =>
            return program
          case None =>
            // Noop
        }
      }

      program
    }

    fixedPoint(solveAny, program)
  }

  /**
    * Resolve every reference in the program.
    *
    * TODO: For L1, this method is sufficient. For L2-3, a reference may depend
    * TODO: upon another reference, and we should give this method more slack.
    *
    * @param program
    * @param language
    * @return
    */
  def solveResolve(program: Program)(implicit language: Language): List[Program] = {
    def solveResolveInner(program: Program)(implicit language: Language): List[Program] = {
      if (program.resolve.isEmpty) {
        List(program)
      } else {
        for (resolve <- program.resolve) {
          val declarations = Graph(program.constraints).res(program.resolution)(resolve.n1)

          if (declarations.nonEmpty) {
            return declarations.toList.flatMap(declaration =>
              resolve.n2
                .unify(declaration)
                .map(program.substitute)
                .map(_.addResolution(resolve.n1 -> declaration))
                .map(mergeOccurrences(_)(resolve.n1, declaration))
            )
          }
        }

        Nil
      }
    }

    // TODO: After solving a resolve, we need to solveAny to propagate knowledge

    def solveResolves(programs: List[Program]) = {
      programs.flatMap(solveResolveInner)
    }

    fixedPoint(solveResolves, List(program))
  }

  /**
    * After resolving a reference to a declaration, we merge the two
    * occurrences by making their names equal.
    *
    * @param program
    * @param o1
    * @param o2
    * @return The updated program and updated name.
    */
  def mergeOccurrences(program: Program)(o1: Occurrence, o2: Occurrence): Program = (o1, o2) match {
    case (Occurrence(_, p1@TermAppl("NameVar", List(x)) , _), Occurrence(_, p2@TermString(y), _)) =>
      program.rewrite(topdown(attempt(new Strategy {
        override def apply(p: Pattern): Option[Pattern] = {
          if (p == p1) {
            Some(p2)
          } else {
            None
          }
        }
      })))
    case (Occurrence(_, p1@TermAppl("NameVar", List(a)), _), Occurrence(_, p2@TermAppl("NameVar", List(b)), _)) =>
      val newname = nameProvider.next

      program.rewrite(topdown(attempt(new Strategy {
        override def apply(p: Pattern): Option[Pattern] = {
          if (p == p1 || p == p2) {
            Some(TermString(s"n$newname"))
          } else {
            None
          }
        }
      })))
    case (Occurrence(_, p2@TermString(x), _), Occurrence(_, p1@TermAppl("NameVar", List(a)), _)) =>
      program.rewrite(topdown(attempt(new Strategy {
        override def apply(p: Pattern): Option[Pattern] = {
          if (p == p1) {
            Some(p2)
          } else {
            None
          }
        }
      })))
    case _ =>
      program
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

  def mergePatterns(program: Program)(p1: Pattern, p2: Pattern)(implicit language: Language): Option[Program] = {
    p1.unify(p2).map(program.substitute)
  }

  def mergeTypes(program: Program)(t1: Option[Pattern], t2: Option[Pattern])(implicit language: Language): Option[Program] = (t1, t2) match {
    case (None, None) =>
      program
    case (Some(t1), Some(t2)) =>
      t1.unify(t2).map(program.substitute)
    case _ =>
      None
  }

  def mergeScopes(program: Program)(ss1: List[Pattern], ss2: List[Pattern])(implicit language: Language): Option[Program] = {
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
