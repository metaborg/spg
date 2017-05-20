package org.metaborg.spg.core.terms

import org.spoofax.interpreter.terms._
import org.spoofax.terms._

object Converters extends DecorateAsJava with DecorateAsScala

/**
  * Adds an `asJava` method that converts a Scala term to a Java term.
  */
trait DecorateAsJava {
  val termFactory = new TermFactory()

  implicit def termAsJavaTerm(term: Term): Decorators.AsJava[IStrategoTerm] = {
    new Decorators.AsJava(asJavaTerm(term))
  }

  /**
    * Convert a Scala term to a Java term.
    *
    * The Scala library represents lists as applications of Cons/Nil, whereas
    * the Java library represents lists as first-class citizens. This method
    * takes care of this conversion.
    *
    * @param term
    * @return
    */
  def asJavaTerm(term: Term): IStrategoTerm = term match {
    case TermString(s) =>
      termFactory.makeString(s)
    case TermAppl("Cons", List(x, xs)) =>
      // TODO: Get rid of casts
      termFactory.makeListCons(asJavaTerm(x.asInstanceOf[Term]), asJavaTerm(xs.asInstanceOf[Term]).asInstanceOf[IStrategoList])
    case TermAppl("Nil", Nil) =>
      termFactory.makeList()
    case TermAppl(constructor, children) =>
      val javaConstructor = termFactory.makeConstructor(constructor, children.length)
      val javaChildren = children.map(child => asJavaTerm(child.asInstanceOf[Term])).toArray

      termFactory.makeAppl(javaConstructor, javaChildren, termFactory.makeList())
  }
}

trait DecorateAsScala {
  implicit def termAsScalaTerm(term: IStrategoTerm): Decorators.AsScala[Pattern] = {
    new Decorators.AsScala(asScalaTerm(term))
  }

  /**
    * Convert Spoofax's Java ATerm to SPG's Scala ATerm representation.
    *
    * SPG's ATerm lacks first-class lists. Instead, lists are presented as
    * applications of the Cons/Nil operator.
    *
    * @param term
    * @return
    */
  def asScalaTerm(term: IStrategoTerm): Pattern = term match {
    case s: IStrategoString =>
      TermString(s.stringValue())
    case a: IStrategoAppl =>
      TermAppl(a.getConstructor.getName, a.getAllSubterms.map(asScalaTerm).toList)
    case l: IStrategoList =>
      if (l.size() == 0) {
        TermAppl("Nil")
      } else {
        TermAppl("Cons", List(asScalaTerm(l.head()), asScalaTerm(l.tail())))
      }
  }
}

trait Decorators {

  /**
    * Generic class containing the `asJava` converter method.
    *
    * @param op
    * @tparam A
    */
  class AsJava[A](op: => A) {
    /**
      * Converts a Scala term to the corresponding Java term.
      *
      * @return
      */
    def asJava: A = op
  }

  /**
    * Generic class containing the `asScala` converter method.
    *
    * @param op
    * @tparam A
    */
  class AsScala[A](op: => A) {
    /**
      * Converts a Java collection to the corresponding Scala collection.
      *
      * @return
      */
    def asScala: A = op
  }

}

object Decorators extends Decorators
