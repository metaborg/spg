package nl.tudelft

import nl.tudelft.fragments.spoofax.models.{Sort, SortVar}

import scala.annotation.tailrec
import scala.util.Random

package object fragments {
  type TermBinding = Map[TermVar, Pattern]
  type ScopeBinding = Map[Scope, Scope]
  type SortBinding = Map[SortVar, Sort]
  type SeenImport = List[Pattern]
  type SeenScope = List[Scope]

  // An instance of the NameProvider made globally available
  val nameProvider = NameProvider(9)

  // Implicitly convert Option[State] to List[State]
  implicit def optionToList(o: Option[State]): List[State] = o match {
    case None => Nil
    case Some(x) => List(x)
  }

  // Implicitly convert State to List[State]. Allows returning a State when an List[State] is required.
  implicit def stateToList(s: State): List[State] = List(s)

  // Implicitly define methods on any sequence
  implicit class RichSeq[T](seq: Seq[T]) {
    // Get random element from the sequence
    def random: T =
      seq(Random.nextInt(seq.length))

    // Get random element from the sequence
    def randomOption: Option[T] = seq match {
      case Nil => None
      case _ => Some(seq(Random.nextInt(seq.length)))
    }

    // Zip with a function
    def zipWith[U](f: T => U): Seq[(T, U)] =
      seq.map(x => (x, f(x)))
  }

  implicit class RichList[T](list: List[T]) {
    // Fold until the accumulator becomes None
    def foldLeftWhile[B](z: B)(f: (B, T) => Option[B]): Option[B] = list match {
      case x :: xs =>
        f(z, x).flatMap(r => xs.foldLeftWhile(r)(f))
      case _ =>
        Some(z)
    }

    // A hybrid of map and foldLeft (TODO: I like 'mapAccum' better, courtesy of Sven)
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
        list.zip(scopes).foldLeftWhile(Map.empty[Scope, Scope]) {
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

    def unify(types: List[Pattern]): Option[TermBinding] =
      if (list.length == types.length) {
        list.zip(types).foldLeftWhile(Map.empty[TermVar, Pattern]) {
          case (termBinding, (t1, t2)) =>
            t1.unify(t2, termBinding)
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

    def substitute(binding: TermBinding): List[Constraint] =
      list.map(_.substitute(binding))

    def substituteSort(binding: SortBinding): List[Constraint] =
      list.map(_.substituteSort(binding))

    def substituteScope(binding: ScopeBinding): List[Constraint] =
      list.map(_.substituteScope(binding))
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

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = {
    @tailrec def repeatAcc(acc: T, n: Int): T = n match {
      case 0 => acc
      case _ => repeatAcc(f(acc), n - 1)
    }

    (t: T) => repeatAcc(t, n)
  }

  // Computes the fixed point by repeated application of f on x
  @tailrec def fixedPoint[T](f: T => T, x: T): T = f(x) match {
    case fx if x == fx =>
      x
    case fx =>
      fixedPoint(f, fx)
  }
}
