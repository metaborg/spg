package org.metaborg.spg

package object solver {
  /**
    * Implicitly convert a State to a List[State].
    */
  implicit def stateToList(s: State): List[State] = {
    List(s)
  }

  /**
    * Implicitly convert an Option[State] to a List[State].
    */
  implicit def optionToList(o: Option[State]): List[State] = {
    o.map(List(_)).getOrElse(Nil)
  }

  /**
    * Implicitly convert an Option[List[State]] to a List[State].
    */
  implicit def listToOption(o: Option[List[State]]): List[State] = {
    o.getOrElse(Nil)
  }
}
