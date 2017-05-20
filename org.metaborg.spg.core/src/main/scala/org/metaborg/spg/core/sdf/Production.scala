package org.metaborg.spg.core.sdf

case class Production(sort: Sort, rhs: List[Symbol], attributes: List[Attribute] = Nil, cons: Option[String] = None) {
  lazy val isReject: Boolean = {
    attributes.contains(Reject())
  }

  lazy val isBracket: Boolean = {
    attributes.contains(Bracket())
  }
}
