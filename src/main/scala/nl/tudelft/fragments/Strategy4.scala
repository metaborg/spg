package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.models.{Signature, SortAppl}
import nl.tudelft.fragments.spoofax.{Language, Printer, Specification}

object Strategy4 {
  def main(args: Array[String]): Unit = {
    val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")

    // Make the various language specifications implicitly available
    implicit val productions = language.productions
    implicit val signatures = language.signatures
    implicit val specification = language.specification
    implicit val printer = language.printer
    implicit val rules = specification.rules

    val kb = repeat(gen, 200)(rules)

    for (i <- 1 to 100) {
      val rule = kb.filter(_.sort == SortAppl("Start")).random

      val term = complete(kb, rule)

      println(term)

      /*
      if (term.isDefined) {
        val concrete = Concretor.concretize(term.get, term.get.state.facts)

        val aterm = Converter.toTerm(concrete)

        println(print(aterm))
      }
      */
    }
  }

  def gen(rules: List[Rule])(implicit signatures: List[Signature]): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.randomOption

    // Lazily merge a random other rule $r \in rules$ into $rule$, solving $recurse$
    val mergedOpt = recurseOpt.flatMap(recurse =>
      rules.shuffle.view
        .flatMap(rule.merge(recurse, _))
        .find(_.pattern.size < 10)
    )

    // Attempt to resolve a resolution constraint in the merged fragment
    mergedOpt.map(merged => {
      // Get resolution constraints
      val ress = merged
        .resolve
        .shuffle
        .view

      // Pair each resolution constraint with the possible declarations
      val declarations = ress
        .flatMap(res =>
          Graph(merged.state.facts).resolves(Nil, res.n1, merged.state.nameConstraints).map(dec =>
            (res, dec)
          )
        )
        .view

      // Resolve the resolution constraint
      val resolved = declarations
        .flatMap { case (res, dec) =>
          Builder.resolve(merged, res, dec._3).map(resolved => (res, dec, resolved))
        }

      // Resolve & check consistency
      resolved.headOption.map(_._3).map(_ :: rules)
        .getOrElse(merged :: rules)
    }).getOrElse(rules)
  }

  // Complete the given rule by solving resolution & recurse constraints
  def complete(rules: List[Rule], rule: Rule)(implicit signatures: List[Signature]): Option[Rule] = {
    if (rule.resolve.nonEmpty) {
      val choices = resolveRandom(rules, rule)

      val smallChoices = choices
        .filter(choice => choice.pattern.size + choice.recurse.size <= 20)

      for (choice <- smallChoices) {
        if (choice.resolve.isEmpty && choice.recurse.isEmpty) {
          return Some(choice)
        } else {
          val deeper = complete(rules, choice)

          if (deeper.isDefined) {
            return deeper
          }
        }
      }
    } else if (rule.recurse.nonEmpty) {
      for (recurse <- rule.recurse) {
        val choices = rules.flatMap(rule.merge(recurse, _))

        val smallChoices = choices
          .filter(choice => choice.pattern.size + choice.recurse.size <= 20)

        if (smallChoices.isEmpty) {
          return None
        } else {
          for (choice <- smallChoices) {
            if (choice.resolve.isEmpty && choice.recurse.isEmpty) {
              return Some(choice)
            } else {
              val deeper = complete(rules, choice)

              if (deeper.isDefined) {
                return deeper
              }
            }
          }
        }
      }
    } else {
      Seq(rule).view
    }

    None
  }

  // Resolve a random resolution constraint
  def resolveRandom(rules: List[Rule], rule: Rule)(implicit signatures: List[Signature]): List[Rule] = {
    val res = rule.resolve

    res.flatMap(res => {
      val resolveInternal = Builder.resolve(rule, res, null)
      val resolveExternal = Builder.buildToResolve(rules, rule)

      if (resolveInternal.nonEmpty) {
        Some(resolveInternal.random) // TODO: Is random a good choice? Shouldn't we take all choices in parallel?
      } else if (resolveExternal.nonEmpty) {
        Some(resolveExternal.random._7) // TODO: Is random a good choice? Shouldn't we take all choices in parallel?
      } else {
        None
      }
    })
  }

  // Solve a random recurse constraint
  def solveRandom(rules: List[Rule], rule: Rule)(implicit signatures: List[Signature]): List[Rule] = {
    val randomRules = rules.shuffle

    rule.recurse.flatMap(rec =>
      randomRules.flatMap(rule.merge(rec, _))
    )
  }
}
