package org.metaborg.spg.core.spoofax

import org.spoofax.interpreter.terms.IStrategoTerm

object SpoofaxScala {

  implicit class RichStrategoTerm(term: IStrategoTerm) {
    // Collect all terms on which f succeeds
    def collectAll(f: IStrategoTerm => Boolean): List[IStrategoTerm] =
      if (f(term)) {
        term :: term.getAllSubterms.toList.flatMap(_.collectAll(f))
      } else {
        term.getAllSubterms.toList.flatMap(_.collectAll(f))
      }

    // Collect the first term on which f succeeds
    def collect(f: IStrategoTerm => Boolean): Option[IStrategoTerm] =
      if (f(term)) {
        Some(term)
      } else {
        term.getAllSubterms.foldLeft(Option.empty[IStrategoTerm]) {
          case (x@Some(_), _) =>
            x
          case (None, subterm) =>
            subterm.collect(f)
        }
      }
  }

}
