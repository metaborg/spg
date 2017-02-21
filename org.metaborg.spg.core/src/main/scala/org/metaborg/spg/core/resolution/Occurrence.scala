package org.metaborg.spg.core.resolution

import org.metaborg.spg.core.terms.{Pattern, TermAppl, TermString}

/**
  * Representation of a name in the scope graph. Contains a namespace, name,
  * and position. The name is of type Pattern, because it may be a variable.
  *
  * @param namespace
  * @param name
  * @param position
  */
case class Occurrence(namespace: String, name: Pattern, position: Int) {
  /**
    * Check if an occurrence is ground.
    *
    * An occurrence is ground if is not a name variable. We do not support
    * occurrences with name variables inside of terms.
    *
    * @return
    */
  def isGround: Boolean = name match {
    case TermAppl("NameVar", _) =>
      false
    case _ =>
      true
  }
}

object OccurrenceImplicits {
  /**
    * Implicitly convert the pattern-representation of an occurrence to the
    * Occurrence-representation of an occurrence.
    *
    * @param term
    * @return
    */
  implicit def patternToOccurrence(term: Pattern): Occurrence = {
    Occurrence(getNamespace(term(0)), term(1), getPosition(term(2)))
  }

  implicit def occurrenceToPattern(occurrence: Occurrence): Pattern = {
    TermAppl("Occurrence", List(
      TermString(occurrence.namespace), occurrence.name, TermString(String.valueOf(occurrence.position))
    ))
  }

  implicit class RichPattern(p: Pattern) {
    def occurrence: Occurrence = {
      patternToOccurrence(p)
    }
  }

  /**
    * Get namespace from term-representation of occurrence.
    *
    * @param term
    * @return
    */
  def getNamespace(term: Pattern): String = term match {
    case TermAppl("Default", _) =>
      "Default"
    case TermAppl("All", _) =>
      "All"
    case TermString(namespace) =>
      namespace
  }

  /**
    * Get position of term-representation of occurrence.
    *
    * @param term
    * @return
    */
  def getPosition(term: Pattern): Int = {
    Integer.valueOf(toString(term))
  }

  def toString(term: Pattern): String = term match {
    case termString: TermString =>
      termString.name
    case _ =>
      throw new RuntimeException("I don't know how to make this a string.")
  }
}
