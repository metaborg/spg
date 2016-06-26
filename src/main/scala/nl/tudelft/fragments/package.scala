package nl.tudelft

import scala.util.Random

package object fragments {
  type TermBinding = Map[TermVar, Pattern]
  type TypeBinding = Map[TypeVar, Type]
  type ScopeBinding = Map[ScopeVar, Scope]
  type NameBinding = Map[NameVar, Name]
  type SortBinding = Map[SortVar, Sort]
  type ConcreteBinding = Map[SymbolicName, ConcreteName]
  type Path = List[PathElem]
  type SeenImport = List[Name]
  type SeenScope = List[Scope]

  // An instance of the NameProvider made globally available
  val nameProvider = NameProvider(9)

  // Implicitly convert Option[T] to List[T]. Allows returning an option when a list is required.
  implicit def optionToList[T](o: Option[T]): List[T] = o match {
    case None => Nil
    case Some(x) => List(x)
  }

  // Implicitly convert State to List[State]. Allows returning a State when an List[State] is required.
  implicit def stateToList(s: State): List[State] = List(s)

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

    // Pair consecutive elements. I.e. turn [1,2,3, ...] into [(1,2); (2,3), ...]
    def pairs: List[(T, T)] = list match {
      case x1 :: x2 :: xs => (x1, x2) :: (x2 :: xs).pairs
      case _ => Nil
    }

    // Get random element from the list
    def random: T =
      list(Random.nextInt(list.length))

    // Get random element from the list
    def safeRandom: Option[T] = list match {
      case Nil => None
      case _ => Some(list(Random.nextInt(list.length)))
    }

    // Shuffle elements of the list
    def shuffle: List[T] =
      Random.shuffle(list)

    // Get a random subset of the list
    def randomSubset(n: Int): List[T] =
      Random.shuffle(list).take(n)

    // Remove element from list
    def -(elem: T): List[T] =
      list diff List(elem)
  }

  implicit class RichScopeList[T <: Scope](list: List[T]) extends RichList[T](list) {
    def unify(scopes: List[Scope]): Option[ScopeBinding] =
      if (list.length == scopes.length) {
        list.zip(scopes).foldLeftWhile(Map.empty[ScopeVar, Scope]) {
          case (scopeBinding, (s1, s2)) =>
            s1.unify(s2, scopeBinding)
        }
      } else {
        None
      }

    def substituteScope(binding: ScopeBinding): List[Scope] =
      list.map(_.substituteScope(binding))

    def freshen(scopeBinding: Map[String, String]): (Map[String, String], List[Scope]) =
      this.mapFoldLeft(scopeBinding) { case (nameBinding, scope) =>
        scope.freshen(nameBinding)
      }
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

    def unify(types: List[Type]): Option[(TypeBinding, NameBinding)] =
      if (list.length == types.length) {
        list.zip(types).foldLeftWhile(Map.empty[TypeVar, Type], Map.empty[NameVar, Name]) {
          case ((typeBinding, nameBinding), (t1, t2)) =>
            t1.unify(t2, typeBinding, nameBinding)
        }
      } else {
        None
      }
  }

  implicit class RichConstraintList[T <: Constraint](list: List[T]) extends RichList[T](list) {
    def freshen(nameBinding: Map[String, String]): (Map[String, String], List[Constraint]) =
      this.mapFoldLeft(nameBinding) { case (nameBinding, constraint) =>
        constraint.freshen(nameBinding)
      }

    def substituteSort(binding: SortBinding): List[Constraint] =
      list.map(_.substituteSort(binding))

    def substituteType(binding: TypeBinding): List[Constraint] =
      list.map(_.substituteType(binding))

    def substituteScope(binding: ScopeBinding): List[Constraint] =
      list.map(_.substituteScope(binding))

    def substituteName(binding: NameBinding): List[Constraint] =
      list.map(_.substituteName(binding))

    def substituteConcrete(binding: ConcreteBinding): List[Constraint] =
      list.map(_.substituteConcrete(binding))
  }

  implicit class RichNamingConstraintList[T <: NamingConstraint](list: List[T]) extends RichList[T](list) {
    def freshen(nameBinding: Map[String, String]): (Map[String, String], List[NamingConstraint]) =
      this.mapFoldLeft(nameBinding) { case (nameBinding, constraint) =>
        constraint.freshen(nameBinding)
      }
  }

  implicit class RichEqList[T <: Eq](list: List[T]) extends RichList[T](list) {
    def substituteConcrete(binding: ConcreteBinding): List[Eq] =
      list.map(_.substituteConcrete(binding))
  }

  implicit class RichDiseqList[T <: Diseq](list: List[T]) extends RichList[T](list) {
    def substituteConcrete(binding: ConcreteBinding): List[Diseq] =
      list.map(_.substituteConcrete(binding))
  }

  // CPS for Tuple2
  implicit class RichTuple2[T1, T2](tuple2: Tuple2[T1, T2]) {
    def map[T3](f: ((T1, T2)) => T3) =
      f(tuple2._1, tuple2._2)
  }

  // Extract a random element from a list
  object ~ {
    def unapply[T](xs: List[T]): Some[T] =
      Some(xs.random)
  }

}
