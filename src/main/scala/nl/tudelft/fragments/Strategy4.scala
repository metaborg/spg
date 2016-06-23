package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.Signatures.Decl
import nl.tudelft.fragments.spoofax.{Converter, Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

object Strategy4 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  def main(args: Array[String]): Unit = {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
    )

    implicit val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.0.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
    )

    val print = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L3/"
    )

    val kb = repeat(gen, 100)(rules)

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

  def gen(rules: List[Rule])(implicit signatures: List[Decl]): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.safeRandom

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
        .resolutionConstraints
        .shuffle
        .view

      // Pair each resolution constraint with the possible declarations
      val declarations = ress
        .flatMap(res =>
          Graph.resolves(Nil, res.n1, merged.constraints, merged.state.nameConstraints).map(dec =>
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
  def complete(rules: List[Rule], rule: Rule)(implicit signatures: List[Decl]): Option[Rule] = {
    if (rule.resolutionConstraints.nonEmpty) {
      val choices = resolveRandom(rules, rule)

      val smallChoices = choices
        .filter(choice => choice.pattern.size + choice.recurse.size <= 20)

      for (choice <- smallChoices) {
        if (choice.resolutionConstraints.isEmpty && choice.recurse.isEmpty) {
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
            if (choice.resolutionConstraints.isEmpty && choice.recurse.isEmpty) {
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
  def resolveRandom(rules: List[Rule], rule: Rule)(implicit signatures: List[Decl]): List[Rule] = {
    val res = rule.resolutionConstraints

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
  def solveRandom(rules: List[Rule], rule: Rule)(implicit signatures: List[Decl]): List[Rule] = {
    val randomRules = rules.shuffle

    rule.recurse.flatMap(rec =>
      randomRules.flatMap(rule.merge(rec, _))
    )
  }

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = n match {
    case 0 => (e: T) => e
    case _ => (e: T) => {
      println(n);
      repeat(f, n - 1)(f(e))
    }
  }
}
