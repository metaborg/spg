package nl.tudelft.fragments

import scala.util.Random

object Solver {
  import Graph._

  // Solve one constraint
  def solve(c: Constraint, cs: List[Constraint], all: List[Constraint], ts: TypeEnv): Option[Substitution] = c match {
    case TypeOf(n@SymbolicName(_), t) =>
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
    case Res(n1@SymbolicName(_), n2@NameVar(_)) =>
      for ((_, dec, cond) <- Random.shuffle(resolves(n1, all))) {
        val result = solve(cs.substituteName(Map[NameVar, Name](n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts)

        if (result.isDefined) {
          return Some((result.get._1, result.get._2 ++ Map(n2 -> dec), result.get._3 ++ cond))
        }
      }

      None
    case AssocConstraint(n@SymbolicName(_), s@ScopeVar(_)) =>
      val scope = associated(n, all)

      if (scope.isDefined) {
        solve(cs.substituteScope(Map(s -> scope.get)), all.substituteScope(Map(s -> scope.get)), ts)
      } else {
        None
      }
    case _ =>
      None
  }

  // Solve all constraints non-deterministically, but give TypeEquals and TypeOf(SymbolicName(_), _) precedence
  def solve(C: List[Constraint], all: List[Constraint], ts: TypeEnv): Option[Substitution] =
    if (C.nonEmpty) {
      if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName]).get
        val result = solve(c, C-c, all, ts)

        if (result.isDefined) {
          return result
        }
      } else if (C.exists(_.isInstanceOf[TypeEquals])) {
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

case class TypeEnv(bindings: Map[SymbolicName, Type] = Map.empty) {
  def contains(n: SymbolicName): Boolean =
    bindings.contains(n)

  def apply(n: SymbolicName) =
    bindings(n)

  def +(e: (SymbolicName, Type)) =
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
  def declarations(s: Scope, all: List[Constraint]): List[SymbolicName] =
    all.flatMap {
      case Dec(`s`, n@SymbolicName(_)) =>
        Some(n)
      case _ =>
        None
    }

  // Scope for name
  def scope(n: SymbolicName, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case Dec(s, `n`) =>
        Some(s)
      case Ref(`n`, s) =>
        Some(s)
      case _ =>
        None
    }

  // Scope associated with a declaration
  def associated(n: SymbolicName, all: List[Constraint]): Option[Scope] =
    all.flatMap {
      case AssocFact(`n`, s) =>
        Some(s)
      case _ =>
        None
    }.headOption

  // Direct imports
  def dimports(s: Scope, all: List[Constraint]): List[Scope] =
    all.flatMap {
      case DirectImport(`s`, s2) =>
        Some(s2)
      case _ =>
        None
    }

  // Parent edge
  def edgeParent(s: Scope, all: List[Constraint]): List[(Path, Scope)] =
    parent(s, all).flatMap(path(_, all)).map { case (path, scope) =>
      (Parent() :: path, scope)
    }

  // Direct Import edge
  def edgeDImport(s: Scope, all: List[Constraint]): List[(Path, Scope)] =
    dimports(s, all).flatMap(imports => path(imports, all).map((imports, _))).map { case (imports, (path, scope)) =>
      (Import(imports) :: path, scope)
    }

  // Transitive reflexive closure of edge relation // TODO: Include named imports
  def path(s: Scope, all: List[Constraint]): List[(Path, Scope)] =
    List((Nil, s)) ++ edgeParent(s, all) ++ edgeDImport(s, all)

  // Reachable declarations
  def reachable(s: Scope, all: List[Constraint]): List[(Path, SymbolicName)] =
    path(s, all).flatMap { case (path, scope) =>
      declarations(scope, all).map(dec => (path, dec))
    }

  // Visible declarations
  def visible(s: Scope, all: List[Constraint]): List[(Path, SymbolicName, List[Condition])] =
    reachable(s, all).map { case (path, name) =>
      val condition = reachable(s, all)
        .filter(_._1.length < path.length)
        .map { case (_, other) => Diseq(name, other) }

      (path, name, condition)
    }

  // Resolution
  def resolves(n: SymbolicName, all: List[Constraint]): List[(Path, SymbolicName, List[Condition])] =
    scope(n, all).flatMap(s =>
      visible(s, all).map { case (path, dec, cs) =>
        (path, dec, Eq(n, dec) :: cs)
      }
    )
}

abstract class PathElem
case class Declaration(n: NameVar) extends PathElem
case class Parent() extends PathElem
case class Import(s: Scope) extends PathElem

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
