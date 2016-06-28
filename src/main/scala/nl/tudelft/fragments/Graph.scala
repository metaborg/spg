package nl.tudelft.fragments

import nl.tudelft.fragments.LabelImplicits._

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
  def scope(n: Name) = facts
    .find {
      case Ref(`n`, s) => true
      case _ => false
    }
    .map(_.asInstanceOf[Ref].s)

  // Get declarations for scope
  def declarations(s: Scope): List[Name] = facts.flatMap {
    case Dec(`s`, n) => Some(n)
    case _ => None
  }

  // Get scope associated to name
  def associated(n: Name) = facts
    .find {
      case AssocFact(`n`, s) => true
      case _ => false
    }
    .map(_.asInstanceOf[AssocFact].s)

  // Get named imports for scope
  def imports(s: Scope): List[Name] = facts.flatMap {
    case AssociatedImport(`s`, n) => Some(n)
    case _ => None
  }

  // Get endpoints of l-labeled edges for scope
  def edges(l: Label, s: Scope): List[Scope] = facts.flatMap {
    case DirectEdge(`s`, `l`, d) => Some(d)
    case _ => None
  }

  // Resolution
  def res(R: Resolution)(x: Name): List[(Name, List[NamingConstraint])] =
    res(Nil, R)(x)

  // Resolution
  def res(I: SeenImport, R: Resolution)(x: Name): List[(Name, List[NamingConstraint])] =
    if (R.contains(x)) {
      List((R(x), List.empty[NamingConstraint]))
    } else {
      val D = env(wellFormedness, x :: I, Nil, R)(scope(x).get)

      D.declarations
        .filter { case (y, _) =>
          x.namespace == y.namespace
        }
        .map { case (y, c) =>
          (y, Eq(x, y) :: c)
        }
    }

  // Environment
  def env(re: Regex, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment =
    if (S.contains(s) || re == EmptySet) {
      Environment()
    } else {
      envLabels(re, 'D' :: labels, I, S, R)(s)
    }

  // Environment for labels
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

  // Environment for declarations
  def envDec(re: Regex, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment =
    if (!re.acceptsEmptyString) {
      Environment()
    } else {
      val names = declarations(s).map(x =>
        (x, (declarations(s) - x).map(y => Diseq(x, y)))
      )

      Environment(names)
    }

  // Environment for label
  def envOther(re: Regex, l: Label, I: SeenImport, S: SeenScope, R: Resolution)(s: Scope): Environment = {
    val scopes = IS(l, I, R)(s) ++ edges(l, s).filter(_.vars.isEmpty)

    scopes
      .map(s2 => env(re.derive(l.name), I, s :: S, R)(s2))
      .fold(Environment())(_ union _)
  }

  // Imported scopes
  def IS(l: Label, I: SeenImport, R: Resolution)(s: Scope): List[Scope] =
    (imports(s) diff I)
      .flatMap(y => R.get(y))
      .flatMap(associated(_))
}

abstract class NamingConstraint extends Constraint {
  override def freshen(nameBinding: Map[String, String]): (Map[String, String], NamingConstraint)
}

case class Diseq(n1: Name, n2: Name) extends NamingConstraint {
  def substitute(on1: Name, on2: Name) =
    Diseq(n1.substitute(on1, on2), n2.substitute(on1, on2))

  override def substituteConcrete(binding: ConcreteBinding) =
    Diseq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))

  override def substituteType(binding: TypeBinding): NamingConstraint =
    this

  override def substituteName(binding: NameBinding): NamingConstraint =
    Diseq(n1.substituteName(binding), n2.substituteName(binding))

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

case class Eq(n1: Name, n2: Name) extends NamingConstraint {
  def substitute(on1: Name, on2: Name) =
    Eq(n1.substitute(on1, on2), n2.substitute(on1, on2))

  def substituteConcrete(binding: ConcreteBinding) =
    Eq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))

  override def substituteType(binding: TypeBinding): NamingConstraint =
    this

  override def substituteName(binding: NameBinding): NamingConstraint =
    Eq(n1.substituteName(binding), n2.substituteName(binding))

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
