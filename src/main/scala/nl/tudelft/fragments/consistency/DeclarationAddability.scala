package nl.tudelft.fragments.consistency

import nl.tudelft.fragments.regex.{EmptySet, Regex}
import nl.tudelft.fragments.spoofax.Language
import nl.tudelft.fragments.spoofax.models.Sort
import nl.tudelft.fragments.{CGDecl, CGDirectEdge, CGenRecurse, CResolve, Constraint, Graph, Label, Name, Pattern, Rule, Scope, State, SymbolicName}

/**
  * Check whether for every reference (in isolation) there is either a reachable declaration or there exists a sequence
  * of transformations that adds a reachable declaration for the reference.
  *
  * The ability to do so does not guarantee consistency, but the inability to do so does guarantee failure. This is
  * sound but not complete: we only eliminate inconsistent programs, but let some inconsistent programs get through.
  */
object DeclarationAddability {
  type Path = List[Label]

  // TODO: Either add a declaration or there must already exist a declaration!

  def isConsistent(rule: Rule)(implicit language: Language): Boolean =
    rule.resolve.forall(canAddDeclaration(rule, _))

  /**
    * Determine if we can add a declaration for the given resolve constraint.
    */
  def canAddDeclaration(rule: Rule, resolve: CResolve)(implicit language: Language): Boolean = resolve.n1 match {
    case SymbolicName(ns, name) =>
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
  def declarationability(rws: List[Rule], rule: Rule, s: Scope, ns: String, c: Regex[Label])(implicit language: Language): Boolean =
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
  def declarationability(rws: List[Rule], rule: Rule, recurses: List[CGenRecurse], s: Scope, ns: String, c: Regex[Label], cs: Set[(Sort, Regex[Label])])(implicit language: Language): Either[Set[(Sort, Regex[Label])], Boolean] = {
    var css = cs

    for (recurse <- recurses) {
      val rewrites = rws.filter(rewrite => recurse.sort == rewrite.sort)

      for (rewrite <- rewrites.filter(rw => rule.mergex(recurse, rw, 0).isDefined)) {
        val merged = rule.mergex(recurse, rewrite, 0).get
        val ng = merged._1
        val newRecurses = ng.recurse.diff(rule.recurse.substituteSort(merged._3).substituteScope(merged._5).substitute(merged._4))

        for ((path, scope) <- paths(s.substituteScope(merged._5), ng)) {
          val nc = c.derive(path)

          if (nc.acceptsEmptyString) {
            if (decls(ng, scope).exists(_.asInstanceOf[Name].namespace == ns)) {
              return Right(true)
            }
          }

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
  def sortsForScope(r: Rule, s: Scope): List[Sort] =
    r.recurse.filter(_.scopes.head == s).map(_.sort)

  /**
    * All paths in the graph from scope s
    *
    * @param s
    * @param r
    * @return
    */
  def paths(s: Scope, r: Rule)(implicit language: Language): List[(Path, Scope)] =
    (Nil, s) :: Graphx(r.facts).path(s, language.specification.params.wf) // TODO: Shouldn't the WF predicate be a derivative? I.e. after n steps, its no longer the default WF...

  /**
    * All declarations in the scope s in the graph g
    *
    * @param r
    * @return
    */
  def decls(r: Rule, s: Scope): List[Pattern] =
    Graphx(r.facts).decls(s)
}

// TODO: Replace usage of class below by the Graph class, that supports label ordering and prevents cycles
case class Graphx(constraints: List[Constraint]) {
  type Path = List[Label]

  // Edges from scope s
  def edges(s: Scope): List[CGDirectEdge] = constraints
    .collect { case e@CGDirectEdge(s1, _, s2) if s1 == s => e }

  // Declarations in scope s
  def decls(s: Scope): List[Pattern] = constraints
    .collect { case e@CGDecl(s1, n) if s1 == s => n }

  // Single-step paths from scope s
  def pathDirect(s: Scope): List[(Path, Scope)] = edges(s)
    .map { case CGDirectEdge(_, l, s2) => (List(l), s2) }

  // Multi-step paths with well-formedness wf from scope s. TODO: Fails on cycles! This already happend once!
  def path(s: Scope, wf: Regex[Label]): List[(Path, Scope)] = wf match {
    case EmptySet() =>
      Nil
    case _ =>
      pathDirect(s) ++ pathDirect(s).flatMap { case (label1, scope1) =>
        path(scope1, wf.derive(label1)).map { case (label2, scope2) =>
          (label1 ++ label2, scope2)
        }
      }
  }

  // Reachable declarations
  def reachable(s: Scope, wf: Regex[Label]): List[Pattern] =
    path(s, wf).flatMap { case (_, ss) => decls(ss) }
}
