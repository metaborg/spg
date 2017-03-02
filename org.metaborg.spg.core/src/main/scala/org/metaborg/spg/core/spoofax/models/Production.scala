package org.metaborg.spg.core.spoofax.models

case class Production(sort: Sort, rhs: List[Symbol], attributes: List[Attribute] = Nil, cons: Option[String] = None) {
  def isReject: Boolean =
    attributes.contains(Reject())

  def isBracket: Boolean =
    attributes.contains(Bracket())
}

// Attributes
abstract class Attribute

case class Reject() extends Attribute

case class Bracket() extends Attribute
