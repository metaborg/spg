package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.regex.Regex
import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.Sort
import nl.tudelft.fragments._

/**
  * Check whether for every reference (in isolation) there is either a reachable scope var, reachable declaration or
  * there exists a sequence of transformations that adds a reachable declaration for the reference.
  *
  * The ability to do so does not guarantee consistency, but the inability to do so does guarantee failure. This is
  * sound but not complete: we only eliminate inconsistent programs, but let some inconsistent programs get through.
  *
  * TODO: This check forgets that if we pass two scopes to the constraint generation function, it may connect
  * these two scopes. This is the case for Let/3 in Tiger. The body of the let is not connected until it finishes
  * generating all the declarations.
  */
object DeclarationAddability {
  type Path = List[Label]

  def isConsistent(rule: Rule)(implicit language: Language): Boolean =
    rule.resolve.forall(resolve =>
      reachableScopeVar(rule, resolve) || existsDeclaration(rule, resolve) || canAddDeclaration(rule, resolve)
    )

  /**
    * Determine if there exists a reachable scope var
    */
  def reachableScopeVar(rule: Rule, resolve: CResolve)(implicit language: Language): Boolean = {
    val graph = Graph(rule.state.facts)

    graph.reachableVarScopes(rule.state.resolution)(graph.scope(resolve.n1)).nonEmpty
  }

  /**
    * Determine if there exists a reachable declaration
    */
  def existsDeclaration(rule: Rule, resolve: CResolve)(implicit language: Language): Boolean = {
    val graph = Graph(rule.state.facts)

    graph.res(rule.state.resolution)(resolve.n1).nonEmpty
  }

  /**
    * Determine if we can add a declaration for the given resolve constraint.
    */
  def canAddDeclaration(rule: Rule, resolve: CResolve)(implicit language: Language): Boolean = resolve.n1 match {
    case SymbolicName(ns, name) =>
      declarationability(language.specification.rules, rule, Graph(rule.facts).scope(resolve.n1), ns, language.specification.params.wf)
    case ConcreteName(ns, name, _) =>
      declarationability(language.specification.rules, rule, Graph(rule.facts).scope(resolve.n1), ns, language.specification.params.wf)
  }

  /**
    * Determine if we can add a declaration for the given resolve constraint
    * given continuation c.
    *
    * @param rws
    * @param c
    * @return
    */
  def declarationability(rws: List[Rule], rule: Rule, s: Pattern, ns: String, c: Regex[Label])(implicit language: Language): Boolean =
    declarationability(rws, rule, rule.recurse, s, ns, c, Set.empty) match {
      case Left(_) => false
      case Right(_) => true
    }

  /**
    * Determine whether we can add a declaration with namespace ns that is
    * reachable from scope s with continuation c and seen continuations cs.
    *
    * Returns either a seen set or the boolean true.
    *
    * @param rws Possible rewrites
    * @param c   Continuation c
    * @param cs  Set of seen continuations
    * @return
    */
  def declarationability(rws: List[Rule], rule: Rule, recurses: List[CGenRecurse], s: Pattern, ns: String, c: Regex[Label], cs: Set[(Sort, Regex[Label])])(implicit language: Language): Either[Set[(Sort, Regex[Label])], Boolean] = {
    var css = cs

    // We can expand the term in any of the holes
    for (recurse <- recurses) {
      val rewrites = rws.filter(rewrite => recurse.sort == rewrite.sort)

      // We can expand the hole with any of the rewrite rules
      for (rewrite <- rewrites.filter(rw => rule.mergex(recurse, rw, 0).isDefined)) {
        val merged = rule.mergex(recurse, rewrite, 0).get
        val ng = merged._1
        val newRecurses = ng.recurse.diff(rule.recurse.substituteSort(merged._3).substituteScope(merged._5).substitute(merged._4))

        // For every scope that became reachable in the new graph
        for ((path, scope) <- paths(s.substituteScope(merged._5), ng)) {
          // Compute the well-formedness predicate w.r.t. the reachable scope
          val nc = c.derive(path)

          // If the WF predicate is valid and the reachable scope has declarations, we can add a declaration!
          if (nc.acceptsEmptyString) {
            if (decls(ng, scope).exists(_.asInstanceOf[Name].namespace == ns)) {
              return Right(true)
            }
          }

          // Otherwise, we (recursively) explore the new problem
          val sorts = sortsForScope(ng, scope)

          for (sort <- sorts) {
            if (!css.contains((sort, nc))) {
              // Add to seen set
              css = css + (sort -> nc)

              // Call child
              val recursiveCall = declarationability(rws, ng, newRecurses, scope, ns, nc, css)

              recursiveCall match {
                // Merge seen sets
                case Left(seen) => css = css ++ seen
                case Right(true) => return Right(true)
                case Right(false) => throw new Exception("Illegal state")
              }
            }
          }
        }
      }
    }

    // We failed; return our seen set to the caller
    Left(css)
  }

  /**
    * Get the sorts of the recurses for the given scope s in the graph g.
    *
    * @param r
    * @param s
    * @return
    */
  def sortsForScope(r: Rule, s: Pattern): List[Sort] =
    r.recurse.filter(_.scopes.head == s).map(_.sort)

  /**
    * All paths in the graph from scope s
    *
    * @param s
    * @param r
    * @return
    */
  def paths(s: Pattern, r: Rule)(implicit language: Language): List[(Path, Pattern)] =
    (Nil, s) :: Graphx(r.facts).path(s, language.specification.params.wf)

  /**
    * All declarations in the scope s in the graph g
    *
    * @param r
    * @return
    */
  def decls(r: Rule, s: Pattern): List[Pattern] =
    Graphx(r.facts).decls(s)
}

// TODO: Replace usage of class below by the Graph class, that supports label ordering and named imports
case class Graphx(constraints: List[Constraint]) {
  type Path = List[Label]

  // Edges from scope s
  def edges(s: Pattern): List[CGDirectEdge] = constraints
    .collect { case e@CGDirectEdge(s1, _, s2) if s1 == s => e }

  // Declarations in scope s
  def decls(s: Pattern): List[Pattern] = constraints
    .collect { case e@CGDecl(s1, n) if s1 == s => n }

  // Single-step paths from scope s
  def pathDirect(s: Pattern): List[(Path, Pattern)] = edges(s)
    .map { case CGDirectEdge(_, l, s2) => (List(l), s2) }

  // Multi-step paths with well-formedness wf from scope s
  def path(S: SeenScope)(s: Pattern, wf: Regex[Label]): List[(Path, Pattern)] =
    if (S.contains(s) || wf.rejectsAll) {
      Nil
    } else {
      pathDirect(s) ++ pathDirect(s).flatMap { case (label1, scope1) =>
        path(s :: S)(scope1, wf.derive(label1)).map { case (label2, scope2) =>
          (label1 ++ label2, scope2)
        }
      }
    }

  def path(s: Pattern, wf: Regex[Label]): List[(Path, Pattern)] =
    path(Nil)(s, wf)

  // Reachable declarations
  def reachable(s: Pattern, wf: Regex[Label]): List[Pattern] =
    path(Nil)(s, wf).flatMap { case (_, ss) => decls(ss) }
}
