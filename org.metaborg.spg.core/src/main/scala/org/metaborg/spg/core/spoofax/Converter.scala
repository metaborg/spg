package org.metaborg.spg.core.spoofax

import org.metaborg.spg.core._
import org.metaborg.spg.core.spoofax.models.Strategy
import org.metaborg.spg.core.spoofax.models.Strategy._
import org.metaborg.spg.core.terms.{As, Pattern, TermAppl, TermString}
import org.spoofax.interpreter.terms.{IStrategoConstructor, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoConstructor, StrategoList, StrategoString, TermFactory}

// Convert Pattern to IStrategoTerm
object Converter {
  val termFactory = new TermFactory

  def toTerm(term: Pattern): IStrategoTerm = conssToCons(term) match {
    case As(_, term) =>
      toTerm(term)
    case TermAppl(cons, children) =>
      cons match {
        case "Cons" | "Nil" =>
          consToList(term)
        case "STRING" =>
          termFactory.makeString("X")
        case _ =>
          termFactory.makeAppl(toConstructor(cons, children.length), listToList(children))
      }
    case TermString(name) =>
      toString(name)
  }

  def toConstructor(name: String, arity: Int): IStrategoConstructor =
    new StrategoConstructor(name, arity)

  def toString(value: String): IStrategoString =
    new StrategoString(value, null, 0)

  /**
    * Replace "Conss" by "Cons". We use "Conss" internally as a constructor of
    * sort Iter(a), i.e. non-empty lists.
    *
    * @param term
    * @return
    */
  def conssToCons(term: Pattern): Pattern = {
    val rewrite = new Strategy {
      override def apply(p: Pattern): Option[Pattern] = p match {
        case TermAppl("Conss", children) =>
          Some(TermAppl("Cons", children))
        case _ =>
          None
      }
    }

    topdown(attempt(rewrite))(term).get
  }

  // TermAppl("Cons", List(x, TermAppl("Cons", List(y, TermAppl("Nil"))))) => IStrategoList(x, IStrategoList(y, IStrategoList(null)))
  def consToList(list: Pattern): IStrategoList = list match {
    case TermAppl(cons, children) =>
      cons match {
        case "Cons" =>
          new StrategoList(toTerm(children.head), consToList(children(1)), null, 0)
        case "Nil" =>
          new StrategoList(null, null, null, 0)
      }
  }

  // List(x, y) => IStrategoList(x, IStrategoList(y, IStrategoList(null)))
  def listToList(terms: List[Pattern]): IStrategoList = terms match {
    case x :: xs =>
      new StrategoList(toTerm(x), listToList(xs), null, 0)
    case _ =>
      new StrategoList(null, null, null, 0)
  }
}
