package org.metaborg.spg.core.resolution

import LabelImplicits._
import org.metaborg.spg.core.regex.Regex
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.resolution.OccurrenceImplicits._
import org.metaborg.spg.core.terms.{Pattern, Var}

/**
  * Name resolution on a scope graph.
  *
  * @param facts
  * @param language
  */
case class Graph(facts: List[Constraint])(implicit language: Language) {
  type SeenImport = List[Pattern]
  type SeenScope = List[Pattern]

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
    * Get declarations for scope with namespace.
    *
    * @param scope
    * @param namespace
    * @return
    */
  def declarations(scope: Pattern, namespace: String): List[Occurrence] = {
    declarations(scope).filter(_.namespace == namespace)
  }

  /**
    * Get scope associated to declaration.
    *
    * @param declaration
    * @return
    */
  def associated(declaration: Pattern): Option[Pattern] = {
    facts.collect {
      case CGAssoc(d, scope) if d == declaration =>
        scope
    }.headOption
  }

  /**
    * Get named imports for scope s.
    *
    * @param scope
    * @return
    */
  def imports(scope: Pattern): List[(Label, Pattern)] = {
    facts.collect {
      case CGNamedEdge(s, label, reference) if s == scope =>
        (label, reference)
    }
  }

  /**
    * Get l-labeled named imports for scope s.
    *
    * @param label
    * @param scope
    * @return
    */
  def imports(label: Label, scope: Pattern): List[Pattern] = {
    imports(scope).collect {
      case (l, reference) if l == label =>
        reference
    }
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
    * Get endpoints of l-labeled edges for scope s.
    *
    * @param label
    * @param scope
    * @return
    */
  def edges(label: Label, scope: Pattern): List[Pattern] = {
    edges(scope).collect {
      case (l, s) if l == label =>
        s
    }
  }

  /**
    * Set of declarations to which the reference can resolve.
    *
    * @param R
    * @param x
    * @return
    */
  def res(R: Resolution)(x: Pattern): Set[Occurrence] = {
    res(Nil, R)(x)
  }

  /**
    * Set of declarations to which the reference can resolve.
    *
    * @param I
    * @param R
    * @param reference
    * @return
    */
  def res(I: SeenImport, R: Resolution)(reference: Pattern): Set[Occurrence] = R.get(reference) match {
    case Some(declaration) =>
      Set(declaration.occurrence)
    case None =>
      val D = env(reference)(language.specification.wf, reference :: I, Nil, R)(scope(reference))

      D.declarations.filter(_.namespace == reference.namespace)
  }

  // Set of declarations that are reachable from S with path satisfying re
  def env(reference: Pattern)(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment =
    if (S.contains(s) || re.rejectsAll) {
      Environment()
    } else {
      envLabels(reference)(re, 'D' :: language.specification.labels, I, S, R)(s)
    }

  // Set of declarations visible from S through labels in L after shadowing
  def envLabels(reference: Pattern)(re: Regex[Label], L: List[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = {
    LabelOrdering.max(L, language.specification.order)
      .map(l => envLabels(reference)(re, LabelOrdering.lt(L, l, language.specification.order), I, S, R)(s).shadows(reference, envL(reference)(re, l, I, S, R)(s)))
      .fold(Environment())(_ union _)
  }

  // Multiplex based on label
  def envL(reference: Pattern)(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = l match {
    case Label('D') =>
      envDec(re, I, R)(s)
    case _ =>
      envOther(reference)(re, l, I, S, R)(s)
  }

  // Set of declarations accessible from scope s with a D-labeled step
  def envDec(re: Regex[Label], I: SeenImport, R: Resolution)(s: Pattern): Environment =
    if (!re.acceptsEmptyString) {
      Environment()
    } else {
      Environment(declarations(s))
    }

  // Set of declarations accessible from scope s through an l-labeled edge
  def envOther(reference: Pattern)(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Pattern): Environment = {
    val scopes = IS(l, I, R)(s) ++ edges(l, s).filter(_.vars.isEmpty)

    scopes
      .map(s2 => env(reference)(re.derive(l.name), I, s :: S, R)(s2))
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
    reachableScopes(language.specification.wf, Nil, Nil, R)(s)

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
    reachableVarScopes(language.specification.wf, Nil, Nil, R)(s)

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
