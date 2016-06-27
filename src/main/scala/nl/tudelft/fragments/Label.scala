package nl.tudelft.fragments

// Representation of a label
case class Label(name: Char)

// A partial ordering on labels
case class LabelOrdering(relation: List[(Label, Label)]) extends PartialOrdering[Label] {
  override def tryCompare(x: Label, y: Label): Option[Int] = {
    val findA = relation.find(_ == (x, y)).map(_ => -1)
    val findB = relation.find(_ == (y, x)).map(_ => 1)

    findA.orElse(findB)
  }

  override def lteq(x: Label, y: Label): Boolean =
    tryCompare(x, y).exists(_ => true)
}

object LabelOrdering {
  // Get all labels that do not have a larger label
  def max(L: List[Label], ordering: LabelOrdering) =
    L.filter(l => !L.exists(l2 => ordering.lt(l, l2)))

  // Get all labels that are less than l
  def lt(L: List[Label], l: Label, ordering: LabelOrdering) =
    L.filter(l2 => ordering.lt(l2, l))
}

object LabelImplicits {
  implicit def charToLabel(char: Char): Label =
    Label(char)
}
