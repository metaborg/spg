package org.metaborg.spg.core.spoofax.models

/**
  * A constructor is an operation in an algebraic signature.
  *
  * @param arguments
  * @param target
  */
case class Operation(name: String, arguments: List[Sort], target: Sort) extends Constructor {
  /**
    * A constructor's arity is its number of argument sorts.
    */
  lazy val arity: Int = {
    arguments.size
  }

  /**
    * @inheritdoc
    */
  lazy val sorts: List[Sort] = {
    (target :: arguments).distinct
  }

  /**
    * @inheritdoc
    */
  override def substitute(binding: Map[SortVar, Sort]): Operation = {
    Operation(name, arguments.map(_.substituteSort(binding)), target.substituteSort(binding))
  }
}
