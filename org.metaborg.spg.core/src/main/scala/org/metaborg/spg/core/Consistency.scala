package org.metaborg.spg.core

import org.metaborg.spg.core.resolution.{Graph, Label, Occurrence}
import org.metaborg.spg.core.solver.{CEqual, CFalse, CGDecl, CGDirectEdge, CGRef, CGenRecurse, CInequal, CResolve, Constraint, FSubtype}
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.terms.Pattern
import org.metaborg.spg.core.resolution.OccurrenceImplicits._

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
    lazy val c3 = advancedResolveCheck(program)

    c1 && c2 && c3
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

  /**
    * Check that for every reference, at least one of the following holds:
    *  (- there is a reachable declaration)
    *  - there is a hole through which it is possible to add a reachable declaration
    *
    * @param program
    * @param language
    * @return
    */
  private def advancedResolveCheck(program: Program)(implicit language: Language): Boolean = {
    def canAddDeclaration(resolve: CResolve, recurse: CGenRecurse): Boolean = {
      // Easy case; single-rule check, sufficient for L0
      for (rule <- language.rulesMem(recurse.name, recurse.sort)) {
        val r = rule.instantiate()

        for (scope <- r.scopes) {
          val declarations = GraphX(r.constraints).reachableDeclarations(scope)

          if (declarations.nonEmpty) {
            return true
          }
        }
      }

      false
    }

    program.resolve.forall(resolve => {
      lazy val c1 = Graph(program.constraints).res(program.resolution)(resolve.n1).nonEmpty

      lazy val c2 = program.recurse
        .filter(recurse =>
          // TODO: Only consider recurses that are parametrized by a scope that is reachable from the reference..
          true
        )
        .exists(recurse =>
          // TODO: Also take WF from reference to parametrized scope into account
          canAddDeclaration(resolve, recurse)
        )

      c1 || c2
    })
  }
}

case class GraphX(facts: List[Constraint])(implicit language: Language) {
  /**
    * Get scope for reference.
    *
    * @param reference
    * @return
    */
  def scope(reference: Pattern): Pattern = {
    facts.collect {
      case CGRef(r, scope) if r == reference =>
        scope
    }.head
  }

  /**
    * Get endpoints for any direct edge for scope s.
    *
    * @param scope
    * @return
    */
  def edges(scope: Pattern): List[(Label, Pattern)] = {
    facts.collect {
      case CGDirectEdge(s1, label, s2) if s1 == scope =>
        (label, s2)
    }
  }

  /**
    * Get declarations for scope.
    *
    * @param scope
    * @return
    */
  def declarations(scope: Pattern): List[Occurrence] = {
    facts.collect {
      case CGDecl(s, occurrence) if s == scope =>
        occurrence.occurrence
    }
  }

  /**
    * Get reachable declarations for scope.
    *
    * TODO: Deal with cycles
    * TODO: Well-formedness
    * TODO: Named edges
    * TODO: Imports
    *
    * @param scope
    * @return
    */
  def reachableDeclarations(scope: Pattern): List[Occurrence] = {
    declarations(scope) ++ edges(scope).flatMap {
      case (label, target) =>
        reachableDeclarations(target)
    }
  }
}
