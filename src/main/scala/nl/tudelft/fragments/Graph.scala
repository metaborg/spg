package nl.tudelft.fragments

import scala.collection.immutable.Nil

object Graph {
  // Parent scope
  def parent(s: Scope, all: List[Constraint]): List[Scope] = all.flatMap {
    case Par(`s`, p) =>
      Some(p)
    case _ =>
      None
  }

  // Declarations
  def declarations(s: Scope, all: List[Constraint]): List[Name] = all.flatMap {
    case Dec(`s`, n@SymbolicName(_, _)) =>
      Some(n)
    case Dec(`s`, n@ConcreteName(_, _, _)) =>
      Some(n)
    case _ =>
      None
  }

  // Scope for reference
  def scope(n: Name, all: List[Constraint]): List[Scope] = all.flatMap {
    case Ref(`n`, s) =>
      Some(s)
    case _ =>
      None
  }

  // Scope associated with a declaration
  def associated(n: Name, all: List[Constraint]): Option[Scope] = all.flatMap {
    case AssocFact(`n`, s) =>
      Some(s)
    case _ =>
      None
  }.headOption

  // Associated imports
  def aimports(s: Scope, all: List[Constraint]): List[Name] = all.flatMap {
    case AssociatedImport(`s`, n) =>
      Some(n)
    case _ =>
      None
  }

  // Direct imports
  def dimports(s: Scope, all: List[Constraint]): List[Scope] = all.flatMap {
    case DirectImport(`s`, s2) =>
      Some(s2)
    case _ =>
      None
  }

  // Parent edge
  def edgeParent(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Scope, List[Constraint])] =
    parent(s, all).flatMap(path(seen, _, all, conditions)).map {
      case (seen, path, scope, conditions) =>
        (seen, Parent() :: path, scope, conditions)
    }

  // Associated Import edge
  def edgeAImport(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Scope, List[Constraint])] =
    aimports(s, all)
      .filter(ref => !seen.contains(ref))
      .flatMap(ref =>
        resolves(seen, ref, all, conditions).flatMap {
          case (seen, path, dec, conditions) =>
            associated(dec, all).map(scope =>
              (seen, List(AImport(ref, dec)), scope, conditions)
            )
        }
      )

  // Direct Import edge
  def edgeDImport(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Scope, List[Constraint])] =
    dimports(s, all).flatMap(imports =>
      path(seen, imports, all, conditions).map((imports, _))).map {
        case (imports, (seen, path, scope, conditions)) =>
          (seen, Import(imports) :: path, scope, conditions)
      }

  // Transitive reflexive closure of edge relation
  def path(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Scope, List[Constraint])] =
    List((seen, Nil, s, conditions)) ++
      edgeParent(seen, s, all, conditions) ++
      edgeDImport(seen, s, all, conditions) ++
      edgeAImport(seen, s, all, conditions)

  // Reachable declarations
  def reachable(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Name, List[Constraint])] =
    path(seen, s, all, conditions).flatMap {
      case (seen, path, scope, conditions) =>
        declarations(scope, all).map(dec => (seen, path, dec, conditions))
    }

  // Visible declarations
  def visible(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Name, List[Constraint])] = {
    val reachableNames = reachable(seen, s, all, conditions)

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
  def resolves(seen: Seen, n: Name, all: List[Constraint], conditions: List[Constraint]): List[(Seen, Path, Name, List[Constraint])] =
    n match {
      case ConcreteName(namespace, name, pos) =>
        scope(n, all).flatMap(s =>
          visible(n :: seen, s, all, conditions)
            .filter(_._3.namespace == namespace)
            .filter(_._3.name == name)
            .filter { case (_, _, dec, cs) =>
              Consistency.checkNamingConditions(Eq(n, dec) :: cs ++ conditions)
            }
        )
      case SymbolicName(namespace, name) =>
        scope(n, all).flatMap(s =>
          visible(n :: seen, s, all, conditions)
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

abstract class PathElem
case class Declaration(n: NameVar) extends PathElem
case class Parent() extends PathElem
case class Import(s: Scope) extends PathElem
case class AImport(n1: Name, n2: Name) extends PathElem

abstract class NamingConstraint extends Constraint {
//  def substituteConcrete(binding: ConcreteBinding): Condition
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
