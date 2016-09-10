package nl.tudelft.fragments

import nl.tudelft.fragments.Check.Path
import nl.tudelft.fragments.regex.Regex
import nl.tudelft.fragments.spoofax.models.Sort

object Check {
  type Path = List[Label]

  //  // Given a graph G, a well-formedness predicate WF, and a reference x, is it possible to add a declaration such that p: x^R \-> x^D is a well-formed path?
  //  def declarationability(g: Graph, wf: Regex, x: Pattern) =
  //    declarationability(g, g.scope(x), Set(wf))
  //
  //  // Given a graph G, a well-formedness predicate wf, and a scope s, is it possible to add a declaration such that p: S >-> x^D is a well-formed path?
  //  def declarationability(g: Graph, s: Scope, cs: Set[Regex] = Set.empty) = {
  //    for (rewrite <- rewrites) {
  //      for (path <- rewrite(g).paths(s)) {
  //        val c = g.cont(path)
  //        // For every scope, we need the continuation from that scope
  //        // In the newly added piece, we need to compute all paths, filter out paths that violate WF, check for duplicates, and recursively call this procedure
  //      }
  //    }
  //  }

  /**
    * Determine whether we can add a declaration given continuation c.
    *
    * @param rws
    * @param c
    * @return
    */
  def declarationability(rws: List[Rewrite], g: List[Constraint], s: Scope, c: Regex[Label]): Boolean =
    declarationability(rws, g, s, c, Set.empty)

  /**
    * Determine whether we can add a declaration that is reachable from scope s with continuation c and seen
    * continuations cs.
    *
    * @param rws Possible rewrites
    * @param c   Continuation c
    * @param cs  Set of seen continuations
    * @return
    */
  def declarationability(rws: List[Rewrite], g: List[Constraint], s: Scope, c: Regex[Label], cs: Set[(Sort, Regex[Label])]): Boolean = {
    val recurses = g.collect { case x: CGenRecurse => x }

    for (recurse <- recurses) {
      val rewrites = rws.filter(rewrite => recurse.sort == rewrite.sort)

      for (rewrite <- rewrites) {
        val ng = applyRewrite(g, recurse, rewrite)

        for ((path, scope) <- paths(s, ng)) {
          val nc = c.derive(path)

          if (nc.acceptsEmptyString) {
            if (decls(ng, scope).nonEmpty) {
              return true
            }
          }

          val sorts = sortsForScope(ng, scope)

          for (sort <- sorts) {
            if (!cs.contains((sort, nc))) {
              if (declarationability(rws, ng, scope, nc, cs + (sort -> nc))) {
                return true
              }
            }
          }
        }
      }
    }

    false
  }

  /**
    * Get the sorts of the recurses for the given scope s in the graph g.
    *
    * @param g
    * @param s
    * @return
    */
  def sortsForScope(g: List[Constraint], s: Scope): List[Sort] = g
    .collect { case x: CGenRecurse => x }
    .filter(_.scopes.head == s)
    .map(_.sort)

  /**
    * Apply rewrite r to scope recurse.scopes.head in graph g and remove the
    * recurse constraint from g.
    *
    * @param g
    * @param recurse
    * @return
    */
  def applyRewrite(g: List[Constraint], recurse: CGenRecurse, r: Rewrite): List[Constraint] = {
    // Freshen names in the rewrite
    val (_, freshr) = r.freshen()

    // Unify s with ScopeAppl("s") in the rewrite (by convention)
    val unifier = freshr.scope.unify(recurse.scopes.head).get

    // Propagate the unification
    val substituted = freshr.substitute(unifier)

    // Merge g with r
    (g - recurse) ++ substituted.constraints
  }

  /**
    * All paths in the graph from scope s
    *
    * @param s
    * @param g
    * @return
    */
  def paths(s: Scope, g: List[Constraint]): List[(Path, Scope)] =
    (Nil, s) :: Graphx(g).path(s)

  /**
    * All declarations in the scope s in the graph g
    *
    * @param g
    * @return
    */
  def decls(g: List[Constraint], s: Scope): List[Pattern] =
    Graphx(g).decls(s)
}

case class Graphx(constraints: List[Constraint]) {
  // Edges from scope s
  def edges(s: Scope): List[CGDirectEdge] = constraints
    .collect { case e@CGDirectEdge(s1, _, s2) if s1 == s => e }

  // Declarations in scope s
  def decls(s: Scope): List[Pattern] = constraints
    .collect { case e@CGDecl(s1, n) if s1 == s => n }

  // Single-step paths from scope s
  def pathDirect(s: Scope): List[(Path, Scope)] = edges(s)
    .map { case CGDirectEdge(_, l, s2) => (List(l), s2) }

  // Multi-step paths from scope s. TODO: Fails on cycles!
  def path(s: Scope): List[(Path, Scope)] =
    pathDirect(s) ++ pathDirect(s).flatMap { case (label1, scope1) =>
      path(scope1).map { case (label2, scope2) =>
        (label1 ++ label2, scope2)
      }
    }

  // Reachable declarations
  def reachable(s: Scope) =
    path(s).flatMap { case (_, ss) => decls(ss) }
}

case class Rewrite(sort: Sort, scope: Scope, constraints: List[Constraint]) {
  def freshen(nameBinding: Map[String, String] = Map.empty): (Map[String, String], Rewrite) =
    constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
      scope.freshen(nameBinding).map { case (nameBinding, scope) =>
        (nameBinding, Rewrite(sort, scope, constraints))
      }
    }

  def substitute(binding: ScopeBinding): Rewrite =
    Rewrite(sort, scope.substituteScope(binding), constraints.substituteScope(binding))
}
