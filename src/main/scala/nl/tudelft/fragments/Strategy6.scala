package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

// Build programs top-down
object Strategy6 {
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
      nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
      specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
    )

    val print = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L3/"
    )

    val startRules = rules.filter(_.sort == SortAppl("Start"))

    build(startRules.random)
  }

  def build(partial: Rule)(implicit rules: List[Rule], signatures: List[Signatures.Decl]): List[Rule] = {
    if (partial.pattern.size > 15) {
      None
    } else {
      if (partial.recurse.isEmpty) {
        println("Complete program: " + partial)

        val states = Solver.solve(partial.state)

        if (states.isEmpty) {
          //println("Could not solve it..")

          Nil
        } else {
          println("Solved it!")

          List(partial)
        }
      } else {
        val recurse = partial.recurse.random

        rules
          .flatMap(partial.merge(recurse, _))
          .map(rule => {
            if (rule.recurse.isEmpty) {
              if (Solver.solve(rule.state).isEmpty) {
                //println("Consistent but not solvable: " + rule)
              }
            }

            rule
          })
          .flatMap(build)
      }
    }
  }
}
