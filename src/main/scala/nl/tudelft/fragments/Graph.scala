package nl.tudelft.fragments

// TODO: Resolution gives StackOverflow if there is a direct edge from the scope to itself. Use the algorithm from the paper or from Hendrik's thesis?
case class Graph(all: List[Constraint]) {
  // Parent scope
  def parent(s: Scope): List[Scope] = all.flatMap {
    case DirectEdge(`s`, p) =>
      Some(p)
    case _ =>
      None
  }

  // Declarations
  def declarations(s: Scope): List[Name] = all.flatMap {
    case Dec(`s`, n@SymbolicName(_, _)) =>
      Some(n)
    case Dec(`s`, n@ConcreteName(_, _, _)) =>
      Some(n)
    case _ =>
      None
  }

  // Scope for reference
  def scope(n: Name): List[Scope] = all.flatMap {
    case Ref(`n`, s) =>
      Some(s)
    case _ =>
      None
  }

  // Scope associated with a declaration
  def associated(n: Name): Option[Scope] = all.flatMap {
    case AssocFact(`n`, s) =>
      Some(s)
    case _ =>
      None
  }.headOption

  // Associated imports
  def aimports(s: Scope): List[Name] = all.flatMap {
    case AssociatedImport(`s`, n) =>
      Some(n)
    case _ =>
      None
  }

  // Direct imports
  def dimports(s: Scope): List[Scope] = all.flatMap {
    case DirectImport(`s`, s2) =>
      Some(s2)
    case _ =>
      None
  }

  // Parent edge
  def edgeParent(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Scope, List[Constraint])] =
    parent(s).flatMap(path(seen, _, conditions)).map {
      case (seen, path, scope, conditions) =>
        (seen, Parent() :: path, scope, conditions)
    }

  // Associated Import edge
  def edgeAImport(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Scope, List[Constraint])] =
    aimports(s)
      .filter(ref => !seen.contains(ref))
      .flatMap(ref =>
        resolves(seen, ref, conditions).flatMap {
          case (seen, path, dec, conditions) =>
            associated(dec).map(scope =>
              (seen, List(AImport(ref, dec)), scope, conditions)
            )
        }
      )

  // Direct Import edge
  def edgeDImport(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Scope, List[Constraint])] =
    dimports(s).flatMap(imports =>
      path(seen, imports, conditions).map((imports, _))).map {
        case (imports, (seen, path, scope, conditions)) =>
          (seen, Import(imports) :: path, scope, conditions)
      }

  // Transitive reflexive closure of edge relation
  def path(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Scope, List[Constraint])] =
    List((seen, Nil, s, conditions)) ++
      edgeParent(seen, s, conditions) ++
      edgeDImport(seen, s, conditions) ++
      edgeAImport(seen, s, conditions)

  // Reachable declarations
  def reachable(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Name, List[Constraint])] =
    path(seen, s, conditions).flatMap {
      case (seen, path, scope, conditions) =>
        declarations(scope).map(dec => (seen, path, dec, conditions))
    }

  // Visible declarations
  def visible(seen: SeenImport, s: Scope, conditions: List[Constraint]): List[(SeenImport, Path, Name, List[Constraint])] = {
    val reachableNames = reachable(seen, s, conditions)

    reachableNames.map {
      case (seen, path, name, conditions) =>
        val condition = reachableNames
          .filter(_._2.length < path.length)
          .map { case (_, _, other, _) =>
            Diseq(name, other)
          }

        (seen, path, name, condition ++ conditions)
    }
  }

  // Resolution
  def resolves(seen: SeenImport, n: Name, conditions: List[Constraint]): List[(SeenImport, Path, Name, List[Constraint])] = {
    println("Compute resolution")
    n match {
      case ConcreteName(namespace, name, pos) =>
        scope(n).flatMap(s =>
          visible(n :: seen, s, conditions)
            .filter(_._3.namespace == namespace)
            .filter(_._3.name == name)
            .filter { case (_, _, dec, cs) =>
              Consistency.checkNamingConditions(Eq(n, dec) :: cs ++ conditions)
            }
        )
      case SymbolicName(namespace, name) =>
        scope(n).flatMap(s =>
          visible(n :: seen, s, conditions)
            .filter(_._3.namespace == namespace)
            .filter { case (_, _, dec, cs) =>
              Consistency.checkNamingConditions(Eq(n, dec) :: cs ++ conditions)
            }
            .map { case (seen, path, dec, cs) =>
              (seen, path, dec, Eq(n, dec) :: cs)
            }
        )
    }
  }


  // ------------------------------ The real algorithm from "A Theory of Name Resolution" ------------------------------

  def res(nc: List[NamingConstraint], name: Name): List[(Name, List[NamingConstraint])] =
    res(Nil, nc, name)

  def res(I: SeenImport, nc: List[NamingConstraint], ref: Name): List[(Name, List[NamingConstraint])] = {
    val declarations = scope(ref)
      .map(envV(ref :: I, Nil, nc, _))
      .flatMap(_.declarations)
      .filter { case (dec, _) => dec.namespace == ref.namespace }

    declarations
      .map { case x@(dec, c) => (dec, (Eq(ref, dec) :: unequal(ref, declarations - x) ++ c).distinct) }
      .filter { case (_, c) => Consistency.checkNamingConditions(c) }
  }

  def unequal(ref: Name, declarations: List[(Name, List[NamingConstraint])]) =
    declarations.map { case (name, _) => Diseq(ref, name) }

  def envV(I: SeenImport, S: SeenScope, nc: List[NamingConstraint], scope: Scope): Env =
    envL(I, S, nc, scope) shadows envP(I, S, nc, scope)

  def envL(I: SeenImport, S: SeenScope, nc: List[NamingConstraint], scope: Scope): Env =
    envD(I, S, nc, scope) shadows envI(I, S, nc, scope)

  def envD(I: SeenImport, S: SeenScope, nc: List[NamingConstraint], scope: Scope): Env =
    if (S.contains(scope)) {
      Env()
    } else {
      Env(declarations(scope).map((_, nc)))
    }

  def envI(I: SeenImport, S: SeenScope, nc: List[NamingConstraint], scope: Scope): Env =
    if (S.contains(scope)) {
      Env()
    } else {
      (aimports(scope) diff I)
        .flatMap(res(I, nc, _))
        .map { case (n, nc) => (associated(n), nc) }
        .map { case (Some(s), nc) => envL(I, scope :: S, nc, s) }
        .foldLeft(Env()) { _ union _ }
    }

  def envP(I: SeenImport, S: SeenScope, nc: List[NamingConstraint], scope: Scope): Env =
    if (S.contains(scope)) {
      Env()
    } else {
      parent(scope)
        .map(s => envV(I, scope :: S, nc, s))
        .foldLeft(Env()) { _ union _ }
    }
}

// Environment of declarations
case class Env(declarations: List[(Name, List[NamingConstraint])] = Nil) {
  // This environment shadows given environment e
  def shadows(e: Env) =
    Env(declarations ++ e.declarations.map { case (x, c2) =>
      (x, c2 ++ declarations
        .filter { case (y, c1) =>
          x.namespace == y.namespace
        }
        .map { case (y, c1) =>
          Diseq(x, y)
        }
      )
    })

  // Union two environments
  def union(e: Env) =
    Env(declarations ++ e.declarations)
}

// Paths
abstract class PathElem
case class Declaration(n: NameVar) extends PathElem
case class Parent() extends PathElem
case class Import(s: Scope) extends PathElem
case class AImport(n1: Name, n2: Name) extends PathElem

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
