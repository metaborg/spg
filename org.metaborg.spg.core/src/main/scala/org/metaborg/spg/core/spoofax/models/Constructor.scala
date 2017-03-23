package org.metaborg.spg.core.spoofax.models

/**
  * A constructor is either an operation or an injection.
  */
abstract class Constructor {
  /**
    * Get the target sort for the constructor.
    */
  val target: Sort

  /**
    * Get all distinct sorts in the constructor.
    */
  val sorts: List[Sort]

  /**
    * Substitute sort variables by sorts in the constructor.
    *
    * @param binding
    * @return
    */
  def substitute(binding: Map[SortVar, Sort]): Constructor
}
