package org.metaborg.spg.core

import org.metaborg.spg.core.resolution.Graph
import org.metaborg.spg.core.solver.{CEqual, CFalse, CInequal, FSubtype}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.terms.Pattern

object Consistency {
  /**
    * Unsound and complete check if the partial program can be completed.
    *
    * @param program
    * @return
    */
  def check(program: Program)(implicit l: Language, c: Config): Boolean = {
    lazy val c1 = constraintsCheck(program)
    lazy val c2 = resolveCheck(program)

    if (c.consistency) {
      if (c.throwOnUnresolvable && !c2) {
        throw InconsistencyException(program)
      }

      c1 && c2
    } else {
      c1
    }
  }

  /**
    * Check the satisfiability of the constraints.
    *
    * @param program
    * @return
    */
  def constraintsCheck(program: Program): Boolean = program.constraints.forall {
    case CFalse() =>
      false
    case CEqual(t1, t2) =>
      t1.unify(t2).nonEmpty
    case CInequal(t1, t2) =>
      t1.unify(t2).isEmpty
    case FSubtype(t1, t2) if t1.vars ++ t2.vars == Nil =>
      lazy val supertypeExists = program.subtypes.domain.contains(t1)
      lazy val cyclicSubtype = program.subtypes.isSubtype(t2, t1)

      !supertypeExists && !cyclicSubtype
    case _ =>
      true
  }

  /**
    * Check if for every reference at least one of the following holds:
    *  - there is a reachable declaration
    *  - there is a reachable scope variable
    *  - there is a reachable scope that is the parameter to a scope variable
    *
    * @param program
    * @param l
    * @return
    */
  private def resolveCheck(program: Program)(implicit l: Language) = {
    def reachableDeclaration(graph: Graph, rule: Program, ref: Pattern) = {
      graph.res(rule.resolution)(ref).nonEmpty
    }

    def reachableScopeVar(graph: Graph, rule: Program, scope: Pattern) = {
      graph.reachableVarScopes(rule.resolution)(scope).nonEmpty
    }

    def reachableRecurse(graph: Graph, rule: Program, scope: Pattern) = {
      (rule.recurse.flatMap(_.scopes) intersect graph.reachableScopes(rule.resolution)(scope)).nonEmpty
    }

    val graph = Graph(program.constraints)

    program.resolve.forall(resolve => {
      val scope = graph.scope(resolve.n1)

      reachableDeclaration(graph, program, resolve.n1) || reachableScopeVar(graph, program, scope) || reachableRecurse(graph, program, scope)
    })
  }
}
