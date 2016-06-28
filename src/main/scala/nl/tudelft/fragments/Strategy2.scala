package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.Signatures.Decl
import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.util.Random

object Strategy2 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  def main(args: Array[String]): Unit = {
    implicit val signatures = Signatures.read(
      strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
      signaturePath = "/Users/martijn/Projects/scopes-frames/L1/src-gen/signatures/L1-sig.str"
    )

    val rules = Specification.read(
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.0.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L1/trans/analysis/l1.nabl2"
    )

    val printer = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L1/"
    )

    val kb = repeat(gen, 4000)(rules)

    val closedRules = kb.filter(_.recurse.isEmpty)
    println(closedRules)

    for (rule <- closedRules) {
      val resolvedRule = rule.resolutionConstraints.foldLeft(List(rule)) { case (rules, res@CResolve(n1, n2)) =>
        rules.flatMap(Builder.resolve(_, res, null))
      }

      resolvedRule.map(println)
    }
  }

  def gen(rules: List[Rule])(implicit signatures: List[Decl]): List[Rule] = {
    // Pick a random rule
    val rule = rules.random

    // Pick a random recurse constraint
    val recurseOpt = rule.recurse.safeRandom

    // Lazily merge a random other rule $r \in rules$ into $rule$, solving $recurse$
    val mergedOpt = recurseOpt.flatMap(recurse =>
      Random.shuffle(rules).view
        .flatMap(rule.merge(recurse, _))
        .find(_.pattern.size < 20)
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

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = n match {
    case 0 => (e: T) => e
    case _ => (e: T) => {
      println(n); repeat(f, n - 1)(f(e))
    }
  }
}
