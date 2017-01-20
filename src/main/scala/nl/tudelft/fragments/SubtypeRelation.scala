package nl.tudelft.fragments

case class SubtypeRelation(bindings: List[(Pattern, Pattern)] = Nil) {
  def contains(n: Pattern): Boolean =
    bindings.exists(_._1 == n)

  def domain: List[Pattern] =
    bindings.map(_._1)

  def ++(subtypeRelation: SubtypeRelation) =
    SubtypeRelation(bindings ++ subtypeRelation.bindings)

  def ++(otherBindings: List[(Pattern, Pattern)]) =
    SubtypeRelation(bindings ++ otherBindings)

  def +(pair: (Pattern, Pattern)) =
    SubtypeRelation(pair :: bindings)

  // Returns all t2 such that t1 <= t2
  def supertypeOf(t1: Pattern): List[Pattern] =
  t1 :: bindings.filter(_._1 == t1).map(_._2)

  // Returns all t1 such that t1 <= t2
  def subtypeOf(t2: Pattern): List[Pattern] =
  t2 :: bindings.filter(_._2 == t2).map(_._1)

  // Get all t2 such that t1 <: t2
  def get(ty: Pattern): List[Pattern] =
  bindings.filter(_._1 == ty).map(_._2)

  // Checks whether t1 <= t2
  def isSubtype(ty1: Pattern, ty2: Pattern): Boolean =
  ty1 == ty2 || get(ty1).contains(ty2)

  def substitute(termBinding: TermBinding): SubtypeRelation =
    SubtypeRelation(
      bindings.map { case (t1, t2) =>
        t1.substitute(termBinding) -> t2.substitute(termBinding)
      }
    )

  def freshen(nameBinding: Map[String, String]): (Map[String, String], SubtypeRelation) = {
    val freshBindings = bindings.mapFoldLeft(nameBinding) { case (nameBinding, (t1, t2)) =>
      t1.freshen(nameBinding).map { case (nameBinding, t1) =>
        t2.freshen(nameBinding).map { case (nameBinding, t2) =>
          (nameBinding, t1 -> t2)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, SubtypeRelation(bindings))
    }
  }

  override def toString =
    "SubtypeRelation(List(" + bindings.map { case (n1, n2) => s"""Binding($n1, $n2)""" }.mkString(", ") + "))"
}
