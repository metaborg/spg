package org.metaborg.spg.resolution

import LabelImplicits._
import org.metaborg.spg.collection.DisjointSet
import org.metaborg.spg.regex.Regex
import org.metaborg.spg.spoofax.Language
import org.metaborg.spg.{ConcreteName, Name, Pattern, SymbolicName, Var}
import org.metaborg.spg.solver._

/**
  * Name resolution on a scope graph.
  *
  * @param facts
  * @param language
  */
case class Graph(facts: List[Constraint])(implicit language: Language) {
  type SeenImport = List[Pattern]
  type SeenScope = List[Pattern]

  // Get scope for reference
  def scope(n: Pattern): Pattern = facts
    .collect { case CGRef(`n`, s) => s }
    .head

  // Get declarations for scope
  def declarations(s: Pattern): List[Pattern] = facts
    .collect { case CGDecl(`s`, n) => n }

  // Get declarations for scope with namespace
  def declarations(s: Pattern, ns: String): List[Pattern] = facts
    .collect { case CGDecl(`s`, n) => n }
    .filter(_.asInstanceOf[Name].namespace == ns)

  // Get scope associated to name
  def associated(n: Pattern): Option[Pattern] = facts
    .collect { case CGAssoc(`n`, s) => s }
    .headOption

  // Get named imports for scope s
  def imports(s: Pattern): List[(Label, Pattern)] = facts
    .collect { case CGNamedEdge(`s`, l, n) => (l, n) }

  // Get l-labeled named imports for scope s
  def imports(l: Label, s: Pattern): List[Pattern] = facts
    .collect { case CGNamedEdge(`s`, `l`, n) => n }

  // Get endpoints of l-labeled edges for scope s
  def edges(l: Label, s: Pattern): List[Pattern] = facts
    .collect { case CGDirectEdge(`s`, `l`, s2) => s2 }

  // Get endpoints for any direct edge for scope s
  def edges(s: Pattern): List[(Label, Pattern)] = facts
    .collect { case CGDirectEdge(`s`, l, s2) => (l, s2) }

  // Set of declarations to which the reference can resolve
  def res(R: Resolution)(x: Pattern): Set[(List[NamingConstraint], Pattern)] =
    res(Nil, R)(x)

  // Set of declarations to which the reference can resolve
  def res(I: SeenImport, R: Resolution)(reference: Pattern): Set[(List[NamingConstraint], Pattern)] = R.get(reference) match {
    case Some(declaration) =>
      Set((Nil, declaration))
    case None =>
      // TODO: We can compute `env` given `E`, which allows us to discard some declarations earlier instead of filtering afterwards
      val E = namingConstraints(R)
      val D = env(language.specification.params.wf, reference :: I, Nil, R)(scope(reference))

      D.declarations.map {
        case (cs, declaration) =>
          (Eq(reference, declaration) :: cs ++ E, declaration)
      }.filter {
        case (_, declaration) =>
          reference.isInstanceOf[Name] && declaration.isInstanceOf[Name] && reference.asInstanceOf[Name].namespace == declaration.asInstanceOf[Name].namespace
      }.filter {
        case (cs, declaration) =>
          consistent(cs)
      }
  }

  // Derive constraints on the names that must be satisfied to achieve the given resolution in the current graph
  def namingConstraints(resolution: Resolution): List[NamingConstraint] = {
    resolution.bindings.foldLeft(List[NamingConstraint]()) {
      case (constraints, (reference, declaration)) => {
        val D = env(language.specification.params.wf, List(reference), Nil, resolution)(scope(reference))

        D.declarations.find(_._2 == declaration).map {
          case (cs, _) =>
            Eq(reference, declaration) :: cs ++ constraints
        }.get
      }
    }
  }

  /**
    * Check if the naming constraints are consistent.
    *
    * @param cs
    * @return
    */
  def consistent(cs: List[NamingConstraint]): Boolean = {
    val names = cs.flatMap {
      case Eq(n1, n2) =>
        List(n1, n2)
      case Diseq(n1, n2) =>
        List(n1, n2)
    }.distinct

    val (eqs, diseqs) = cs.partition {
      case _: Eq =>
        true
      case _: Diseq =>
        false
    }

    // Put all names in distinct sets
    val disjointSet = DisjointSet(names: _*)

    // Union sets of names that are supposed to be equal
    eqs.foreach {
      case Eq(n1, n2) =>
        disjointSet.union(n1, n2)
    }

    // Union concrete names
    names.filter(_.isInstanceOf[ConcreteName]).groupBy {
      case ConcreteName(namespace, name, _) =>
        (namespace, name)
    }.foreach {
      case ((ns, name), concreteNames) =>
        concreteNames.combinations(2).foreach {
          case List(n1, n2) =>
            disjointSet.union(n1, n2)
        }
    }

    // All names that must be unequal should be in distinct sets
    lazy val c1 = diseqs.forall {
      case Diseq(n1, n2) =>
        disjointSet(n1) != disjointSet(n2)
    }

    // No set may contain two names of distinct names
    lazy val c2 = disjointSet.sets.forall(set =>
      set.toList.combinations(2).forall {
        case List(ConcreteName(ns1, name1, _), ConcreteName(ns2, name2, _)) if ns1 == ns2 =>
          name1 == name2
        case _ =>
          false
      }
    )

    c1 && c2
  }

  // Set of declarations that are reachable from S with path satisfying re
  def env(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment =
    if (S.contains(s) || re.rejectsAll) {
      Environment()
    } else {
      envLabels(re, 'D' :: language.specification.params.labels, I, S, R)(s)
    }

  // Set of declarations visible from S through labels in L after shadowing
  def envLabels(re: Regex[Label], L: List[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = {
    LabelOrdering.max(L, language.specification.params.order)
      .map(l => envLabels(re, LabelOrdering.lt(L, l, language.specification.params.order), I, S, R)(s) shadows envL(re, l, I, S, R)(s))
      .fold(Environment())(_ union _)
  }

  // Multiplex based on label
  def envL(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = l match {
    case Label('D') =>
      envDec(re, I, R)(s)
    case _ =>
      envOther(re, l, I, S, R)(s)
  }

  // Set of declarations accessible from scope s with a D-labeled step
  def envDec(re: Regex[Label], I: SeenImport, R: Resolution)(s: Pattern): Environment =
    if (!re.acceptsEmptyString) {
      Environment()
    } else {
      Environment(declarations(s).map((Nil, _)))
    }

  // Set of declarations accessible from scope s through an l-labeled edge
  def envOther(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = {
    val scopes = IS(l, I, R)(s) ++ edges(l, s).filter(_.vars.isEmpty)

    scopes
      .map(s2 => env(re.derive(l.name), I, s :: S, R)(s2))
      .fold(Environment())(_ union _)
  }

  // Scopes that are accessible through a nominal l-labeled edge
  def IS(l: Label, I: SeenImport, R: Resolution)(s: Pattern): List[Pattern] =
    (imports(l, s) diff I)
      .flatMap(R.get)
      .flatMap(associated)

  // Scopes that are accessible through a nominal edge
  def IS(I: SeenImport, R: Resolution)(s: Pattern): List[(Label, Pattern)] =
    imports(s)
      .filter { case (_, y) => !I.contains(y) }
      .flatMap { case (l, y) => R.get(y).map(d => (l, d)) }
      .flatMap { case (l, d) => associated(d).map(s2 => (l, s2)) }

  /**
    * Get scopes reachable from s. Ignores variable scopes.
    */
  def reachableScopes(R: Resolution)(s: Pattern): List[Pattern] =
    reachableScopes(language.specification.params.wf, Nil, Nil, R)(s)

  /**
    * Get scopes reachable from s. Ignores variable scopes.
    */
  def reachableScopes(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): List[Pattern] =
    if (S.contains(s)) {
      Nil
    } else {
      val currentScope = if (re.acceptsEmptyString) {
        List(s)
      } else {
        Nil
      }

      val importedScopes = IS(I, R)(s)
      val directScopes = edges(s).filter {
        case (label, targetScope) =>
          targetScope.vars.isEmpty
      }

      currentScope ++ (importedScopes ++ directScopes).flatMap {
        case (l, scope) =>
          reachableScopes(re.derive(l.name), I, s :: S, R)(scope)
      }
    }

  /**
    * Get variable scopes reachable from s.
    */
  def reachableVarScopes(R: Resolution)(s: Pattern): List[Pattern] =
    reachableVarScopes(language.specification.params.wf, Nil, Nil, R)(s)

  /**
    * Get variable scopes reachable from s.
    */
  def reachableVarScopes(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): List[Pattern] =
    if (S.contains(s)) {
      Nil
    } else {
      val currentScope = if (!re.rejectsAll && s.isInstanceOf[Var]) {
        List(s)
      } else {
        Nil
      }

      val importedScopes = IS(I, R)(s)
      val directScopes = edges(s)

      currentScope ++ (importedScopes ++ directScopes).flatMap {
        case (l, scope) =>
          reachableVarScopes(re.derive(l.name), I, s :: S, R)(scope)
      }
    }
}
