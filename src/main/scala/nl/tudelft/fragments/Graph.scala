package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._
import nl.tudelft.fragments.regex.Regex
import nl.tudelft.fragments.spoofax.Language

// Resolution algorithm
case class Graph(facts: List[Constraint])(implicit language: Language) {
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
  def res(R: Resolution)(x: Pattern): List[Pattern] =
    res(Nil, R)(x)

  // Set of declarations to which the reference can resolve
  def res(I: SeenImport, R: Resolution)(x: Pattern): List[Pattern] =
    if (R.contains(x)) {
      List(R(x))
    } else {
      val D = env(language.specification.params.wf, x :: I, Nil, R)(scope(x))

      D.declarations.filter(y =>
        x.isInstanceOf[Name] && y.isInstanceOf[Name] && x.asInstanceOf[Name].namespace == y.asInstanceOf[Name].namespace
      )
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
      Environment(declarations(s))
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
      .flatMap(associated(_))

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
