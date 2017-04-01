package org.metaborg.spg.core

import com.google.inject.Inject
import org.metaborg.spg.core.spoofax.Language
import org.metaborg.spg.core.stratego.Strategy
import org.metaborg.spg.core.stratego.Strategy.{attempt, topdown}
import org.metaborg.spg.core.terms._

import scala.util.Random

class SyntaxShrinker @Inject() (generator: SyntaxGenerator, language: Language)(implicit random: Random) {
  /**
    * Given a term, construct possible smaller terms.
    *
    * @param term
    */
  def shrink(term: Pattern): List[(Pattern, String)] = {
    val allNodes = nodes(term)

    val shrunkPrograms = allNodes.shuffle.flatMap(node => {
      // Infer sort for node in term
      val sort = language.signature.sortForPattern(term, node)

      // Try to generate a sentence for the nodes sort that is strictly smaller.
      generator.generateTry(sort.get, node.size - 1).map(smallerNode =>
        term.rewrite(topdown(attempt(new Strategy {
          override def apply(p: Pattern) = {
            if (p eq node) {
              Some(smallerNode)
            } else {
              None
            }
          }
        })))
      )
    })

    shrunkPrograms.zipWith(tree => {
      language.printer(spoofax.Converter.toTerm(tree))
    })
  }

  /**
    * Get all patterns in the pattern.
    *
    * @param pattern
    * @return
    */
  def nodes(pattern: Pattern): List[Pattern] = {
    pattern match {
      case As(_, t) =>
        pattern :: nodes(t)
      case TermAppl(_, children) =>
        pattern :: children.flatMap(nodes)
      case TermString(_) =>
        List(pattern)
      case Var(_) =>
        List(pattern)
    }
  }
}

