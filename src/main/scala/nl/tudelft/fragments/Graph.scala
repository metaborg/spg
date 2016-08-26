package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._

// Resolution algorithm
case class Graph(/*wellFormedness: Regex, labels: List[Label], labelOrdering: LabelOrdering,*/ facts: List[Constraint]) {
  val labels =
    List(Label('P'), Label('I'))

  val wellFormedness =
    (Character('P') *) ~ (Character('I') *)

  val labelOrdering =
    LabelOrdering(
      (Label('D'), Label('P')),
      (Label('D'), Label('I')),
      (Label('I'), Label('P'))
    )

  // Get scope for reference
  def scope(n: Pattern) = facts
    .find {
      case CGRef(`n`, s) => true
      case _ => false
    }
    .map(_.asInstanceOf[CGRef].s)

  // Get declarations for scope
  def declarations(s: Scope): List[Pattern] =
    facts.collect { case CGDecl(`s`, n) => n }

  // Get scope associated to name
  def associated(n: Pattern) = facts
    .find {
      case CGAssoc(`n`, s) => true
      case _ => false
    }
    .map(_.asInstanceOf[CGAssoc].s)

  // Get named imports for scope s
  def imports(s: Scope): List[(Label, Pattern)] =
    facts.collect { case CGNamedEdge(`s`, l, n) => (l, n) }

  // Get l-labeled named imports for scope s
  def imports(l: Label, s: Scope): List[Pattern] =
    facts.collect { case CGNamedEdge(`s`, `l`, n) => n }

  // Get endpoints of l-labeled edges for scope s
  def edges(l: Label, s: Scope): List[Scope] =
    facts.collect { case CGDirectEdge(`s`, `l`, s2) => s2 }

  // Get endpoints for any direct edge for scope s
  def edges(s: Scope): List[(Label, Scope)] =
    facts.collect { case CGDirectEdge(`s`, l, s2) => (l, s2) }

  // Set of declarations to which the reference can resolve
  def res(R: Resolution)(x: Pattern): List[(Pattern, List[NamingConstraint])] =
    res(Nil, R)(x)

  // Set of declarations to which the reference can resolve
  def res(I: SeenImport, R: Resolution)(x: Pattern): List[(Pattern, List[NamingConstraint])] =
    if (R.contains(x)) {
      List((R(x), List.empty[NamingConstraint]))
    } else {
      val D = env(wellFormedness, x :: I, Nil, R)(scope(x).get)

      D.declarations
        .filter { case (y, _) =>
          x.isInstanceOf[Name] && y.isInstanceOf[Name] && x.asInstanceOf[Name].namespace == y.asInstanceOf[Name].namespace
        }
        .map { case (y, c) =>
          (y, Eq(x, y) :: c)
        }
    }

  // Set of declarations that are reachable from S with path satisfying re
  def env(re: Regex, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment =
    if (S.contains(s) || re == EmptySet) {
      Environment()
    } else {
      envLabels(re, 'D' :: labels, I, S, R)(s)
    }

  // Set of declarations visible from S through labels in L after shadowing
  def envLabels(re: Regex, L: List[Label], I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = {
    LabelOrdering.max(L, labelOrdering)
      .map(l => envLabels(re, LabelOrdering.lt(L, l, labelOrdering), I, S, R)(s) shadows envL(re, l, I, S, R)(s))
      .fold(Environment())(_ union _)
  }

  // Multiplex based on label
  def envL(re: Regex, l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = l match {
    case Label('D') =>
      envDec(re, I, S, R)(s)
    case _ =>
      envOther(re, l, I, S, R)(s)
  }

  // Set of declarations accessible from scope s with a D-labeled step
  def envDec(re: Regex, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment =
    if (!re.acceptsEmptyString) {
      Environment()
    } else {
      val names = declarations(s).map(x =>
        (x, (declarations(s) - x).map(y => Diseq(x, y)))
      )

      Environment(names)
    }

  // Set of declarations accessible from scope s through an l-labeled edge
  def envOther(re: Regex, l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = {
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
  def reachableScopes(R: Resolution)(s: Scope): List[Scope] = {
    reachableScopes(wellFormedness, Nil, Nil, R)(s)
  }

  // Get scopes reachable from s
  def reachableScopes(re: Regex, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): List[Scope] = {
    val current = if (re.acceptsEmptyString) {
      List(s)
    } else {
      Nil
    }

    val importedScopes = IS(I, R)(s)
    val directScopes = edges(s).filter(_._2.vars.isEmpty)

    current ++ (importedScopes ++ directScopes).flatMap { case (l, s) =>
      reachableScopes(re.derive(l.name), I, s :: S, R)(s)
    }
  }
}

abstract class NamingConstraint extends Constraint {
  override def freshen(nameBinding: Map[String, String]): (Map[String, String], NamingConstraint)
}

case class Diseq(n1: Pattern, n2: Pattern) extends NamingConstraint {
  override def substitute(binding: TermBinding): NamingConstraint =
    Diseq(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): NamingConstraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], NamingConstraint) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Diseq(n1, n2))
      }
    }
}

case class Eq(n1: Pattern, n2: Pattern) extends NamingConstraint {
  override def substitute(binding: TermBinding): NamingConstraint =
    Eq(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): NamingConstraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], NamingConstraint) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Eq(n1, n2))
      }
    }
}
