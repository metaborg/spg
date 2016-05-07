package nl.tudelft

import scala.util.Random

package object fragments {
  type TermBinding = Map[TermVar, Pattern]
  type TypeBinding = Map[TypeVar, Type]
  type ScopeBinding = Map[ScopeVar, Scope]
  type NameBinding = Map[NameVar, NameVar]
  type SortBinding = Map[SortVar, Sort]
  type Substitution = (TypeBinding, NameBinding, List[Diseq])
  type Path = List[Scope]

  implicit class RichList[T](list: List[T]) {
    // Fold until the accumulator becomes None
    def foldLeftWhile[B](z: B)(f: (B, T) => Option[B]): Option[B] = list match {
      case x :: xs =>
        f(z, x).flatMap(r => xs.foldLeftWhile(r)(f))
      case _ =>
        Some(z)
    }

    // A hybrid of map and foldLeft
    def mapFoldLeft[U, V](z: V)(f: (V, T) => (V, U)): (V, List[U]) = list match {
      case x :: xs =>
        val (a1, elem) = f(z, x)
        val (a2, list) = xs.mapFoldLeft(a1)(f)

        (a2, elem :: list)
      case _ =>
        (z, Nil)
    }

    // Get random element from the list
    def random: T =
      list(Random.nextInt(list.length))

    // Remove element from list
    def -(elem: T): List[T] =
      list diff List(elem)
  }

  implicit class RichPatternList[T <: Pattern](list: List[T]) extends RichList[T](list) {
    def freshen(nameBinding: Map[String, String]): (Map[String, String], List[Pattern]) =
      this.mapFoldLeft(nameBinding) { case (nameBinding, pattern) =>
        pattern.freshen(nameBinding)
      }
  }

  implicit class RichTypeList[T <: Type](list: List[T]) extends RichList[T](list) {
    def freshen(nameBinding: Map[String, String]): (Map[String, String], List[Type]) =
      this.mapFoldLeft(nameBinding) { case (nameBinding, typ) =>
        typ.freshen(nameBinding)
      }

    def unify(types: List[Type]): Option[TypeBinding] =
      if (list.length == types.length) {
        list.zip(types).foldLeftWhile(Map.empty[TypeVar, Type]) {
          case (typeBinding, (t1, t2)) =>
            t1.unify(t2, typeBinding)
        }
      } else {
        None
      }
  }

//  implicit class RichScopeList[T <: Scope](list: List[T]) extends RichList[T](list) {
//    def fresh(nameBinding: NameBinding): (NameBinding, List[Scope]) =
//      this.mapFoldLeft(nameBinding) { case (nameBinding, scope) =>
//        scope.fresh(nameBinding)
//      }
//
//    def unify(scopes: List[Scope]): Option[ScopeBinding] =
//      if (list.length == scopes.length) {
//        list.zip(scopes).foldLeftWhile(Map.empty[ScopeVar, Scope]) {
//          case (scopeBinding, (s1, s2)) =>
//            s1.unify(s2, scopeBinding)
//        }
//      } else {
//        None
//      }
//  }

  implicit class RichConstraintList[T <: Constraint](list: List[T]) extends RichList[T](list) {
    def freshen(nameBinding: Map[String, String]): (Map[String, String], List[Constraint]) =
      this.mapFoldLeft(nameBinding) { case (nameBinding, constraint) =>
        constraint.freshen(nameBinding)
      }

    def substituteType(binding: TypeBinding): List[Constraint] =
      list.map(_.substituteType(binding))

    def substituteScope(binding: ScopeBinding): List[Constraint] =
      list.map(_.substituteScope(binding))

    def substituteName(binding: NameBinding): List[Constraint] =
      list.map(_.substituteName(binding))
  }

  // CPS for Tuple2
  implicit class RichTuple2[T1, T2](tuple2: Tuple2[T1, T2]) {
    def map[T3](f: ((T1, T2)) => T3) =
      f(tuple2._1, tuple2._2)
  }
}
