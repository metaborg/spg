package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments._
import org.spoofax.interpreter.terms.{IStrategoConstructor, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoConstructor, StrategoList, StrategoString, TermFactory}

// Convert Pattern to IStrategoTerm
object Converter {
  val termFactory = new TermFactory

  def toTerm(term: Pattern): IStrategoTerm = term match {
    case TermAppl(cons, children) =>
      cons match {
        case "Cons" | "Nil" =>
          consToList(term)
        case _ =>
          termFactory.makeAppl(toConstructor(cons, children.length), listToList(children))
      }
    case TermString(name) =>
      toString(name)
//    case ConcreteName(namespace, name, pos) =>
//      toString(name)
    // TODO: Symbolic names should be replaced before conversion, but this does not always happen yet (bug)
    case SymbolicName(namespace, name) =>
      toString(name)
  }

  def toConstructor(name: String, arity: Int): IStrategoConstructor =
    new StrategoConstructor(name, arity)

  // Cons(x, Cons(y, Nil)) => IStrategoList(x, IStrategoList(y, IStrategoList(null)))
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

  def toString(s: String): IStrategoString =
    new StrategoString(s, null, 0)
}
