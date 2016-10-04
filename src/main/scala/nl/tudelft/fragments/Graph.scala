package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._
import nl.tudelft.fragments.regex.Regex
import nl.tudelft.fragments.spoofax.Language

// Resolution algorithm
case class Graph(facts: List[Constraint])(implicit language: Language) {
  // Get scope for reference
  def scope(n: Pattern): Scope = facts
    .collect { case CGRef(`n`, s) => s }
    .head

  // Get declarations for scope
  def declarations(s: Scope): List[Pattern] = facts
    .collect { case CGDecl(`s`, n) => n }

  // Get declarations for scope with namespace
  def declarations(s: Scope, ns: String): List[Pattern] = facts
    .collect { case CGDecl(`s`, n) => n }
    .filter(_.asInstanceOf[Name].namespace == ns)

  // Get scope associated to name
  def associated(n: Pattern): Option[Scope] = facts
    .collect { case CGAssoc(`n`, s) => s }
    .headOption

  // Get named imports for scope s
  def imports(s: Scope): List[(Label, Pattern)] = facts
    .collect { case CGNamedEdge(`s`, l, n) => (l, n) }

  // Get l-labeled named imports for scope s
  def imports(l: Label, s: Scope): List[Pattern] = facts
    .collect { case CGNamedEdge(`s`, `l`, n) => n }

  // Get endpoints of l-labeled edges for scope s
  def edges(l: Label, s: Scope): List[Scope] = facts
    .collect { case CGDirectEdge(`s`, `l`, s2) => s2 }

  // Get endpoints for any direct edge for scope s
  def edges(s: Scope): List[(Label, Scope)] = facts
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

      D.declarations.filter {
        case (y) =>
          x.isInstanceOf[Name] && y.isInstanceOf[Name] && x.asInstanceOf[Name].namespace == y.asInstanceOf[Name].namespace
      }
    }

  // Set of declarations that are reachable from S with path satisfying re
  def env(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment =
    if (S.contains(s) || re.rejectsAll) {
      Environment()
    } else {
      envLabels(re, 'D' :: language.specification.params.labels, I, S, R)(s)
    }

  // Set of declarations visible from S through labels in L after shadowing
  def envLabels(re: Regex[Label], L: List[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = {
    LabelOrdering.max(L, language.specification.params.order)
      .map(l => envLabels(re, LabelOrdering.lt(L, l, language.specification.params.order), I, S, R)(s) shadows envL(re, l, I, S, R)(s))
      .fold(Environment())(_ union _)
  }

  // Multiplex based on label
  def envL(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = l match {
    case Label('D') =>
      envDec(re, I, R)(s)
    case _ =>
      envOther(re, l, I, S, R)(s)
  }

  // Set of declarations accessible from scope s with a D-labeled step
  def envDec(re: Regex[Label], I: SeenImport, R: Resolution)(s: Scope): Environment =
    if (!re.acceptsEmptyString) {
      Environment()
    } else {
      Environment(declarations(s))
    }

  // Set of declarations accessible from scope s through an l-labeled edge
  def envOther(re: Regex[Label], l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = {
    val scopes = IS(l, I, R)(s) ++ edges(l, s).filter(_.vars.isEmpty)

    scopes
      .map(s2 => env(re.derive(l.name), I, s :: S, R)(s2))
      .fold(Environment())(_ union _)
  }

  // Scopes that are accessible through a nominal l-labeled edge
  def IS(l: Label, I: SeenImport, R: Resolution)(s: Scope): List[Scope] =
    (imports(l, s) diff I)
      .flatMap(R.get)
      .flatMap(associated(_))

  // Scopes that are accessible through a nominal edge
  def IS(I: SeenImport, R: Resolution)(s: Scope): List[(Label, Scope)] =
    imports(s)
      .filter { case (_, y) => !I.contains(y) }
      .flatMap { case (l, y) => R.get(y).map(d => (l, d)) }
      .flatMap { case (l, d) => associated(d).map(s2 => (l, s2)) }

  // Get scopes reachable from s
  def reachableScopes(R: Resolution)(s: Scope): List[Scope] =
    reachableScopes(language.specification.params.wf, Nil, Nil, R)(s)

  // Get scopes reachable from s
  def reachableScopes(re: Regex[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): List[Scope] =
    if (S.contains(s)) {
      Nil
    } else {
      val current = if (re.acceptsEmptyString) {
        List(s)
      } else {
        Nil
      }

      val importedScopes = IS(I, R)(s)
      val directScopes = edges(s).filter(_._2.vars.isEmpty)

      current ++ (importedScopes ++ directScopes).flatMap { case (l, scope) =>
        reachableScopes(re.derive(l.name), I, s :: S, R)(scope)
      }
    }
}
