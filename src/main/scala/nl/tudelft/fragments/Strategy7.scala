package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.annotation.tailrec

object Strategy7 {
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

    //println(repeat((x: List[Rule]) => build(x.random), 10)(startRules))
    println(build(startRules.random))
  }

  def build(partial: Rule)(implicit rules: List[Rule], signatures: List[Signatures.Decl]): Option[Rule] = {
    if (partial.recurse.isEmpty) {
      Some(partial)
    } else {
      for (recurse <- partial.recurse) {
        for (rule <- rules) {
          val merged = partial
            .merge(recurse, rule)
            .filter(_.pattern.size <= 20)

          if (merged.isDefined && merged.get.recurse.size <= 5) {
            val continue = build(merged.get)

            if (continue.isDefined) {
              return continue
            }
          }
        }
      }

      return None
    }
  }

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = {
    @tailrec def repeatAcc(acc: T, n: Int): T = n match {
      case 0 => acc
      case _ => println(n); repeatAcc(f(acc), n - 1)
    }

    (t: T) => repeatAcc(t, n)
  }
}
