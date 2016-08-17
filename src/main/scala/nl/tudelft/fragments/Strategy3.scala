package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.Signatures.Decl
import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.util.Random

object Strategy3 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  implicit val signatures = Signatures.read(
    strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
    signaturePath = "/Users/martijn/Projects/scopes-frames/L1/src-gen/signatures/L1-sig.str"
  )

  implicit val specification = Specification.read(
    nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.0.0-SNAPSHOT.spoofax-language!/",
    specPath = "/Users/martijn/Projects/scopes-frames/L1/trans/analysis/l1.nabl2"
  )

  implicit val rules: List[Rule] = specification.rules

  def main(args: Array[String]): Unit = {
    for (i <- 1 to 100) {
      println(gen(rules.random))
    }
  }

  def gen(rule: Rule)(implicit rules: List[Rule], signatures: List[Decl]): Option[Rule] = {
    if (rule.recurse.isEmpty && rule.resolutionConstraints.isEmpty) {
      Some(rule)
    } else if (rule.recurse.nonEmpty) {
      val choices = rules
        .shuffle
        .view
        .flatMap(other => {
          rule
            .merge(rule.recurse.head, other)
            .filter(_.state.pattern.size < 30)
        })

      for (choice <- choices) {
        val complete = choices.headOption.flatMap(gen)

        if (complete.isDefined) {
          return complete
        }
      }

      None
    } else {
      None
    }
  }
}
