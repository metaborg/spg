package org.metaborg.spg

package object solver {
  /**
    * Implicitly convert a State to a List[State].
    */
  implicit def stateToList(s: Program): List[Program] = {
    List(s)
  }

  /**
    * Implicitly convert an Option[State] to a List[State].
    */
  implicit def optionToList(o: Option[Program]): List[Program] = {
    o.map(List(_)).getOrElse(Nil)
  }

  /**
    * Implicitly convert an Option[List[State]] to a List[State].
    */
  implicit def listToOption(o: Option[List[Program]]): List[Program] = {
    o.getOrElse(Nil)
  }
}
