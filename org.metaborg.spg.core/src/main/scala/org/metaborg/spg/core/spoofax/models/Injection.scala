package org.metaborg.spg.core.spoofax.models

/**
  * An injection is a nameless constructor of arity one.
  *
  * @param argument
  * @param target
  */
case class Injection(argument: Sort, target: Sort) extends Constructor {
  /**
    * @inheritdoc
    */
  lazy val sorts: List[Sort] = {
    List(target, argument).distinct
  }

  /**
    * @inheritdoc
    */
  override def substitute(binding: Map[SortVar, Sort]): Injection = {
    Injection(argument.substituteSort(binding), target.substituteSort(binding))
  }
}
