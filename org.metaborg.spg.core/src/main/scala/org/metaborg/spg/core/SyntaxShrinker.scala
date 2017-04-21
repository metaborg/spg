package org.metaborg.spg.core

import com.google.inject.Inject
import org.metaborg.spg.core.spoofax.{Language, ParseService}
import org.metaborg.spg.core.stratego.Strategy
import org.metaborg.spg.core.stratego.Strategy.{attempt, topdown}
import org.metaborg.spg.core.terms._
import rx.lang.scala.Observable

import scala.util.Random
import terms.Converters._

class SyntaxShrinker @Inject() (generator: SyntaxGenerator, parseService: ParseService, language: Language)(implicit random: Random) {
  /**
    * Given a program, construct an observable of smaller programs.
    *
    * @param program
    * @return
    */
  def shrink(program: String): Observable[String] = {
    // 1. Parse program (String => IStrategoTerm)
    val javaTerm = parseService.parse(language.implementation, program)

    // 2. Convert to Scala (IStrategoTerm => Term)
    val scalaTerm = javaTerm.asScala

    // 3. Remove amb-nodes by picking an arbitrary alternative
    val nonambiguous = disambiguate(scalaTerm)

    // 3. Shrink the Scala term (Term => Observable[Term])
    val scalaShrunkTerms = shrink(nonambiguous)

    // 4. Convert to Java (Term => IStrategoTerm) (TODO: Fix runtime cast hack)
    val javaShrunkTerms = scalaShrunkTerms.map(_.asInstanceOf[Term].asJava)

    // 5. Unparse tree (IStrategoTerm => String)
    val shrunkPrograms = javaShrunkTerms.map(language.printer)

    shrunkPrograms
  }

  /**
    * Disambiguate a pattern by choosing a random alternative.
    *
    * @param term
    * @return
    */
  def disambiguate(term: Pattern): Pattern = term match {
    case TermAppl("amb", List(TermAppl("Cons", List(x, _)))) =>
      disambiguate(x)
    case TermAppl(cons, children) =>
      TermAppl(cons, children.map(disambiguate))
    case _ =>
      term
  }

  /**
    * Given a term, construct possible smaller terms.
    *
    * Smaller terms are constructed lazily such that you don't need to compute
    * all smaller terms when you are merely looking for a single smaller term
    * that is ambiguous.
    *
    * @param term
    */
  def shrink(term: Pattern): Observable[Pattern] = {
    Observable
      .from(subTerms(term).shuffle)
      .flatMap(shrink(term, _))
  }

  /**
    * Shrink the given subTerm in the given term.
    *
    * @param term
    * @param subTerm
    * @return
    */
  def shrink(term: Pattern, subTerm: Pattern): Observable[Pattern] = {
    // Infer sort for subTerm in term
    val sort = language.signature.sortForPattern(term, subTerm)

    // Try to generate a sentence for the nodes sort that is strictly smaller.
    val result = generator.generateTry(sort.get, subTerm.size - 1).map(smallerNode =>
      term.rewrite(topdown(attempt(new Strategy {
        override def apply(p: Pattern): Option[Pattern] = {
          if (p eq subTerm) {
            Some(smallerNode)
          } else {
            None
          }
        }
      })))
    )

    Observable.from(result)
  }

  /**
    * Get all patterns in the pattern.
    *
    * @param pattern
    * @return
    */
  def subTerms(pattern: Pattern): List[Pattern] = {
    pattern match {
      case As(_, t) =>
        pattern :: subTerms(t)
      case TermAppl(_, children) =>
        pattern :: children.flatMap(subTerms)
      case TermString(_) =>
        List(pattern)
      case Var(_) =>
        List(pattern)
    }
  }
}

