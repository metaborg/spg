package nl.tudelft.fragments.spoofax.models

abstract class Prod

case class Production(sort: Sort, rhs: List[Symbol]) extends Prod

case class ProductionWithCons(sort: Sort, rhs: List[Symbol], cons: String) extends Prod
