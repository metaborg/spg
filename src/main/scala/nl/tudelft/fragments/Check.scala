package nl.tudelft.fragments

import nl.tudelft.fragments.Check.Path
import nl.tudelft.fragments.regex.Regex

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
  def declarationability(rws: List[Rewrite], c: Regex[Label]): Boolean =
    declarationability(rws, c, Set.empty)

  /**
    * Determine whether we can add a declaration given continuation c and seen
    * continuations cs.
    *
    * @param rws Possible rewrites
    * @param c   Continuation c
    * @param cs  Set of seen continuations
    * @return
    */
  def declarationability(rws: List[Rewrite], c: Regex[Label], cs: Set[Regex[Label]]): Boolean = {
    for (rewrite <- rws) {
      for ((path, scope) <- paths(rewrite)) {
        val nc = c.derive(path)

        if (!nc.acceptsEmptyString) {
          if (!cs.contains(nc)) {
            if (declarationability(rws, nc, cs + nc)) {
              return true
            }
          }
        } else {
          if (decls(rewrite, scope).nonEmpty) {
            return true
          }
        }
      }
    }

    false
  }

  /**
    * All paths in the rewrite from the source scope
    *
    * @param rw
    * @return
    */
  def paths(rw: Rewrite): List[(Path, Scope)] =
    Graphx(rw.constraints).path(ScopeAppl("s"))

  /**
    * All declarations in the scope s in the rewrite rw
    *
    * @param rw
    * @return
    */
  def decls(rw: Rewrite, s: Scope): List[Pattern] =
    Graphx(rw.constraints).decls(s)
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

case class Rewrite(constraints: List[Constraint])

