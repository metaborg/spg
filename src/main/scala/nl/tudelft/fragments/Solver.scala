package nl.tudelft.fragments

import scala.util.Random

object Solver {
  import Graph._

  // Solve single constraint
  def rewrite(c: Constraint, cs: List[Constraint], all: List[Constraint], ts: TypeEnv, conditions: List[Condition]): Solution = c match {
    case TypeOf(n@SymbolicName(_, _), t) =>
      if (ts.contains(n)) {
        List((TypeEquals(ts(n), t) :: cs, all, ts, conditions))
      } else {
        List((cs, all, ts + (n -> t), conditions))
      }
    case TypeOf(n@ConcreteName(_, _, _), t) =>
      if (ts.contains(n)) {
        List((TypeEquals(ts(n), t) :: cs, all, ts, conditions))
      } else {
        List((cs, all, ts + (n -> t), conditions))
      }
    case TypeEquals(t1, t2) =>
      t1.unify(t2).map { case (typeBinding, nameBinding) =>
        (cs.substituteType(typeBinding).substituteName(nameBinding), all.substituteType(typeBinding).substituteName(nameBinding), ts.substituteType(typeBinding), conditions)
      }
    // TODO: stable graph thingy
    case Res(n1@SymbolicName(_, _), n2@NameVar(_)) =>
      // TODO: What if there are no resolvable declarations? Are we allowed to process the constraint?
      // TODO: effect of resolution on incomplete scope graphs?
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all, conditions))) yield {
        // TODO: conditions should be first-class constraints, and they should be conistent
        (cs.substituteName(Map(n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts, cond ++ conditions)
      }
    // TODO: stable graph thingy
    case Res(n1@ConcreteName(_, _, _), n2@NameVar(_)) =>
      for ((_, _, dec, cond) <- Random.shuffle(resolves(Nil, n1, all, conditions))) yield {
        (cs.substituteName(Map[NameVar, Name](n2 -> dec)), all.substituteName(Map(n2 -> dec)), ts, cond ++ conditions)
      }
    case AssocConstraint(n@SymbolicName(_, _), s@ScopeVar(_)) =>
      associated(n, all).map(scope =>
        (cs.substituteScope(Map(s -> scope)), all.substituteScope(Map(s -> scope)), ts, conditions)
      )
    case _ =>
      Nil
  }

  // Solve all constraints non-deterministically, but give TypeEquals(_, _) and TypeOf(SymbolicName(_), _) precedence
  def solve2(C: List[Constraint], all: List[Constraint], ts: TypeEnv, conditions: List[Condition]): Solution =
    if (C.nonEmpty) {
      // Give `TypeOf(SymbolicName(_), _)` precedence
      if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[SymbolicName]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Give `TypeOf(ConcreteName(_), _)` precedence
      else if (C.exists(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName])) {
        val c = C.find(c => c.isInstanceOf[TypeOf] && c.asInstanceOf[TypeOf].n.isInstanceOf[ConcreteName]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Give `TypeEquals(_, _)` precedence
      else if (C.exists(_.isInstanceOf[TypeEquals])) {
        val c = C.find(_.isInstanceOf[TypeEquals]).get
        val result = rewrite(c, C-c, all, ts, conditions)

        result.flatMap((solve2 _).tupled)
      }
      // Try the other constraints
      else {
        for (c <- C) {
          val result = rewrite(c, C-c, all, ts, conditions)

          if (result.nonEmpty) {
            return result.flatMap((solve2 _).tupled)
          }
        }

        None
      }
    } else {
      List((C, all, ts, conditions))
    }

  // Solve all constraints
  def solve(constraints: List[Constraint]): Solution =
    solve2(constraints.filter(_.isProper), constraints, TypeEnv(), Nil)
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
  def edgeParent(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Scope, List[Condition])] =
    parent(s, all).flatMap(path(seen, _, all, conditions)).map { case (seen, path, scope, conditions) =>
      (seen, Parent() :: path, scope, conditions)
    }

  // Associated Import edge
  def edgeAImport(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Scope, List[Condition])] =
    aimports(s, all)
      .filter(ref => !seen.contains(ref))
      .flatMap(ref =>
        resolves(seen, ref, all, conditions).flatMap { case (seen, path, dec, conditions) =>
          associated(dec, all).map(scope =>
            (seen, List(AImport(ref, dec)), scope, conditions)
          )
        }
      )

  // Direct Import edge
  def edgeDImport(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Scope, List[Condition])] =
    dimports(s, all).flatMap(imports => path(seen, imports, all, conditions).map((imports, _))).map {
      case (imports, (seen, path, scope, conditions)) =>
        (seen, Import(imports) :: path, scope, conditions)
    }

  // Transitive reflexive closure of edge relation
  def path(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Scope, List[Condition])] =
    List((seen, Nil, s, Nil)) ++ edgeParent(seen, s, all, conditions) ++ edgeDImport(seen, s, all, conditions) ++ edgeAImport(seen, s, all, conditions)

  // Reachable declarations
  def reachable(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Name, List[Condition])] =
    path(seen, s, all, conditions).flatMap { case (seen, path, scope, conditions) =>
      declarations(scope, all).map(dec => (seen, path, dec, conditions))
    }

  // Visible declarations
  def visible(seen: Seen, s: Scope, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Name, List[Condition])] =
    reachable(seen, s, all, conditions).map { case (seen, path, name, conditions) =>
      val condition = reachable(seen, s, all, conditions)
        .filter(_._2.length < path.length)
        .map { case (_, _, other, _) => Diseq(name, other) }

      (seen, path, name, condition ++ conditions)
    }

  // Resolution
  def resolves(seen: Seen, n: Name, all: List[Constraint], conditions: List[Condition]): List[(Seen, Path, Name, List[Condition])] = n match {
    case ConcreteName(namespace, name, pos) =>
      scope(n, all).flatMap(s =>
        visible(n :: seen, s, all, conditions)
          .filter(_._3.namespace == namespace)
          .filter(_._3.name == name)
          .filter { case (_, _, _, cs) =>
            Consistency.checkNamingConditions(cs ++ conditions)
          }
      )
    case SymbolicName(namespace, name) =>
      scope(n, all).flatMap(s =>
        visible(n :: seen, s, all, conditions)
          .filter(_._3.namespace == n.namespace)
          .filter { case (_, _, _, cs) =>
            Consistency.checkNamingConditions(cs ++ conditions)
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

abstract class Condition {
  def substituteConcrete(binding: ConcreteBinding): Condition
}

case class Diseq(n1: Name, n2: Name) extends Condition {
  def substitute(on1: Name, on2: Name) =
    Diseq(n1.substitute(on1, on2), n2.substitute(on1, on2))

  override def substituteConcrete(binding: ConcreteBinding) =
    Diseq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))
}

case class Eq(n1: Name, n2: Name) extends Condition {
  def substitute(on1: Name, on2: Name) =
    Eq(n1.substitute(on1, on2), n2.substitute(on1, on2))

  def substituteConcrete(binding: ConcreteBinding) =
    Eq(n1.substituteConcrete(binding), n2.substituteConcrete(binding))
}
