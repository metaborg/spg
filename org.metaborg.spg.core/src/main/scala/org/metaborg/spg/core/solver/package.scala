package org.metaborg.spg.core

package object solver {
  /**
    * Implicitly convert a Program to a List[Program].
    */
  implicit def programToList(s: Program): List[Program] = {
    List(s)
  }

  /**
    * Implicitly convert a Program to an Option[Program].
    */
  implicit def programToOption(p: Program): Option[Program] = {
    Option(p)
  }

  /**
    * Implicitly convert an Option[Program] to a List[Program].
    */
  implicit def optionToList(o: Option[Program]): List[Program] = {
    o.map(List(_)).getOrElse(Nil)
  }

  /**
    * Implicitly convert an Option[List[Program]] to a List[Program].
    */
  implicit def listToOption(o: Option[List[Program]]): List[Program] = {
    o.getOrElse(Nil)
  }
}
