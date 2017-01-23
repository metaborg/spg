package org.metaborg.spg

import org.metaborg.spg.regex.Character

// Representation of a label (TODO: deprecate this, use labels with a string instead. We can now take derivative w.r.t label, even if it is a string)
case class Label(name: Char) {
  override def toString =
    s"""Label('$name')"""
}

// A partial ordering on labels
class LabelOrdering(relation: Seq[(Label, Label)]) extends PartialOrdering[Label] {
  override def tryCompare(x: Label, y: Label): Option[Int] = {
    val findA = relation.find(_ == (x, y)).map(_ => -1)
    val findB = relation.find(_ == (y, x)).map(_ => 1)

    findA.orElse(findB)
  }

  override def lteq(x: Label, y: Label): Boolean =
    tryCompare(x, y).exists(_ <= 0)
}

object LabelOrdering {
  // Get all labels that do not have a larger label
  def max(L: List[Label], ordering: LabelOrdering) =
    L.filter(l => !L.exists(l2 => ordering.lt(l, l2)))

  // Get all labels that are less than l
  def lt(L: List[Label], l: Label, ordering: LabelOrdering) =
    L.filter(l2 => ordering.lt(l2, l))

  // Varargs constructor
  def apply(relation: (Label, Label)*): LabelOrdering = {
    new LabelOrdering(relation)
  }
}

object LabelImplicits {
  // Turn a Char (i.e. symbol) into a Label (c.f. resolution)
  implicit def charToLabel(char: Char): Label =
    Label(char)

  // Turn a Label (c.f. resolution) into a Character (i.e. regex)
  implicit def labelToCharacter(label: Label): Character[Label] =
    Character(label)
}
