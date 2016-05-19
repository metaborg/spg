package nl.tudelft.fragments

import scala.collection.immutable.Nil
import scala.util.Random

object Solver {
  import Graph._

  // Solve one constraint
  def solve(c: Constraint, cs: List[Constraint], all: List[Constraint], ts: TypeEnv): Option[Substitution] = c match {
    case TypeOf(n@SymbolicName(_, _), t) =>
      if (ts.contains(n)) {
        solve(TypeEquals(ts(n), t) :: cs, all, ts)
      } else {
        solve(cs, all, ts + (n -> t))
      }
    case TypeOf(n@ConcreteName(_, _, _), t) =>
      if (ts.contains(n)) {
        solve(TypeEquals(ts(n), t) :: cs, all, ts)
      } else {
        solve(cs, all, ts + (n -> t))
      }
    case TypeEquals(t1, t2) =>
      for (
        u <- t1.unify(t2);
        r <- solve(
          cs.substituteType(u._1).substituteName(u._2),
          all.substituteType(u._1).substituteName(u._2),
          ts.substituteType(u._1)
        )
      )
      yield (r._1 ++ u._1, r._2 ++ u._2, r._3)
    // TODO: stable graph thingy
    case Res(n1@SymbolicName(_, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all))) {
        val result = solve(cs.substituteName(Map[NameVar, Name](n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts)

        if (result.isDefined) {
          return Some((result.get._1, result.get._2 ++ Map(n2 -> dec), result.get._3 ++ cond))
        }
      }

      None
    case Res(n1@ConcreteName(_, _, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all))) {
        val result = solve(cs.substituteName(Map[NameVar, Name](n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts)

        if (result.isDefined) {
          return Some((result.get._1, result.get._2 ++ Map(n2 -> dec), result.get._3 ++ cond))
        }
      }

      None
    case AssocConstraint(n@SymbolicName(_, _), s@ScopeVar(_)) =>
      val scope = associated(n, all)

      if (scope.isDefined) {
        solve(cs.substituteScope(Map(s -> scope.get)), all.substituteScope(Map(s -> scope.get)), ts)
      } else {
        None
      }
    case _ =>
      None
  }

  // Solve all constraints non-deterministically, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve(C: List[Constraint], all: List[Constraint], ts: TypeEnv): Option[Substitution] =
    if (C.nonEmpty) {
      // Give `TypeOf(SymbolicName(_), _)` precedence
      if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName]).get
        val result = solve(c, C - c, all, ts)

        if (result.isDefined) {
          return result
        }
      }
      // Give `TypeOf(ConcreteName(_), _)` precedence
      else if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName]).get
        val result = solve(c, C-c, all, ts)

        if (result.isDefined) {
          return result
        }
      }
      // Give `TypeEquals(_, _)` precedence
      else if (C.exists(_.isInstanceOf[TypeEquals])) {
        val c = C.find(_.isInstanceOf[TypeEquals]).get
        val result = solve(c, C-c, all, ts)

        if (result.isDefined) {
          return result
        }
      } else {
        for (c <- C) {
          val result = solve(c, C - c, all, ts)

          if (result.isDefined) {
            return result
          }
        }
      }

      None
    } else {
      Some(Map[TypeVar, Type](), Map[NameVar, NameVar](), List.empty)
    }

  // Solve all constraints
  def solve(constraints: List[Constraint]): Option[Substitution] =
    solve(constraints.filter(_.isProper), constraints, TypeEnv())
}

case class TypeEnv(bindings: Map[Name, Type] = Map.empty) {
  def contains(n: Name): Boolean =
    bindings.contains(n)

  def apply(n: Name) =
    bindings(n)

  def +(e: (Name, Type)) =
    TypeEnv(bindings + e)

  def substituteType(typeBinding: TypeBinding) =
    TypeEnv(bindings.map { case (nameVar, typ) =>
      nameVar -> typ.substituteType(typeBinding)
    })
}

object Graph {
  // Parent scope
  def parent(s: Scope, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case Par(`s`, p) =>
        Some(p)
      case _ =>
        None
    }

  // Declarations
  def declarations(s: Scope, all: List[Constraint]): List[Name] =
    all.flatMap {
      case Dec(`s`, n@SymbolicName(_, _)) =>
        Some(n)
      case Dec(`s`, n@ConcreteName(_, _, _)) =>
        Some(n)
      case _ =>
        None
    }

  // Scope for reference
  def scope(n: Name, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case Ref(`n`, s) =>
        Some(s)
      case _ =>
        None
    }

  // Scope associated with a declaration
  def associated(n: Name, all: List[Constraint]): Option[Scope] =
    all.flatMap {
      case AssocFact(`n`, s) =>
        Some(s)
      case _ =>
        None
    }.headOption

  // Associated imports
  def aimports(s: Scope, all: List[Constraint]): List[Name] =
    all.flatMap {
      case AssociatedImport(`s`, n) =>
        Some(n)
      case _ =>
        None
    }

  // Direct imports
  def dimports(s: Scope, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case DirectImport(`s`, s2) =>
        Some(s2)
      case _ =>
        None
    }

  // Parent edge
  def edgeParent(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Scope, List[Condition])] =
    parent(s, all).flatMap(path(seen, _, all)).map { case (seen, path, scope, conditions) =>
      (seen, Parent() :: path, scope, conditions)
    }

  // Associated Import edge
  def edgeAImport(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Scope, List[Condition])] =
    aimports(s, all)
      .filter(ref => !seen.contains(ref))
      .flatMap(ref =>
        resolves(seen, ref, all).map { case (seen, path, dec, conditions) =>
          (seen, List(AImport(ref, dec)), associated(dec, all).head, conditions)
        }
      )

  // Direct Import edge
  def edgeDImport(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Scope, List[Condition])] =
    dimports(s, all).flatMap(imports => path(seen, imports, all).map((imports, _))).map {
      case (imports, (seen, path, scope, conditions)) =>
        (seen, Import(imports) :: path, scope, conditions)
    }

  // Transitive reflexive closure of edge relation
  def path(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Scope, List[Condition])] =
    List((seen, Nil, s, Nil)) ++ edgeParent(seen, s, all) ++ edgeDImport(seen, s, all) ++ edgeAImport(seen, s, all)

  // Reachable declarations
  def reachable(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Name, List[Condition])] =
    path(seen, s, all).flatMap { case (seen, path, scope, conditions) =>
      declarations(scope, all).map(dec => (seen, path, dec, conditions))
    }

  // Visible declarations
  def visible(seen: Seen, s: Scope, all: List[Constraint]): List[(Seen, Path, Name, List[Condition])] =
    reachable(seen, s, all).map { case (seen, path, name, conditions) =>
      val condition = reachable(seen, s, all)
        .filter(_._2.length < path.length)
        .map { case (_, _, other, _) => Diseq(name, other) }

      (seen, path, name, condition ++ conditions)
    }

  // Resolution
  def resolves(seen: Seen, n: Name, all: List[Constraint]): List[(Seen, Path, Name, List[Condition])] = n match {
    case ConcreteName(namespace, name, pos) =>
      scope(n, all).flatMap(s =>
        visible(n :: seen, s, all)
          .filter(_._3.namespace == namespace)
          .filter(_._3.name == name)
          .map { case (seen, path, dec, cs) =>
            (seen, path, dec, cs)
          }
      )
    case SymbolicName(namespace, name) =>
      scope(n, all).flatMap(s =>
        visible(n :: seen, s, all)
          .filter(_._3.namespace == n.namespace)
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

abstract class Condition {
  def substituteConcrete(binding: ConcreteBinding): Condition
}

case class Diseq(n1: Name, n2: Name) extends Condition {
  override def substituteConcrete(binding: ConcreteBinding) =
    Diseq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))
}

case class Eq(n1: Name, n2: Name) extends Condition {
  def substituteConcrete(binding: ConcreteBinding) =
    Eq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))
}
