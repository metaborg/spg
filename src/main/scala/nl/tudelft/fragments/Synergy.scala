package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models.SortAppl
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

// Synergy is v2 where we combine fragments if it solves constraints.
object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // Make the language parts implicitly available
  implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  // Parameters
  val steps = 2000

  def main(args: Array[String]): Unit = {
    // Grow some programs
    logger.info("Start growing")

    val rules = repeat(grow, steps)(Synergy.rules)

    // Synergize from a random start rule
    logger.info("Start synergizing with " + rules.length + " rules")

    val writer = new PrintWriter(new FileOutputStream(new File("results.log"), true))

    for (i <- 0 to 100) {
      val start = Rule(SortAppl("Program", List()), None, List(ScopeAppl("s118361")), State(TermAppl("Program", List(TermVar("mc"), TermAppl("Cons", List(TermVar("x118362"), TermVar("x118363"))))),List(CGenRecurse(TermVar("mc"),List(ScopeAppl("s118361")),None,SortAppl("MainClass", List())), CGenRecurse(TermVar("x118363"),List(ScopeAppl("s118361")),None,SortAppl("List", List(SortAppl("ClassDecl", List())))), CGenRecurse(TermVar("x118362"),List(ScopeAppl("s118361")),None,SortAppl("ClassDecl", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(List())))
//      val start = Rule(SortAppl("Program", List()), None, List(ScopeAppl("s408940")), State(TermAppl("Program", List(TermAppl("MainClass", List(TermVar("x340230"), TermVar("x340231"), TermAppl("Block", List(TermAppl("Cons", List(TermVar("x408941"), TermVar("x408942"))))))), TermAppl("Cons", List(TermAppl("Class", List(TermVar("x328829"), TermAppl("NoParent", List()), TermAppl("Nil", List()), TermAppl("Cons", List(TermAppl("Method", List(TermAppl("ClassType", List(TermVar("x457612"))), TermVar("x457613"), TermVar("x457614"), TermVar("x457615"), TermVar("x457616"), TermVar("x457617"))), TermAppl("Cons", List(TermVar("x431751"), TermAppl("Nil", List()))))))), TermVar("x118363"))))),List(CGenRecurse(TermVar("x118363"),List(ScopeAppl("s408940")),None,SortAppl("List", List(SortAppl("ClassDecl", List())))), CGenRecurse(TermVar("x408942"),List(ScopeAppl("s408940")),None,SortAppl("List", List(SortAppl("Statement", List())))), CGenRecurse(TermVar("x408941"),List(ScopeAppl("s408940")),None,SortAppl("Statement", List())), CGenRecurse(TermVar("x431751"),List(ScopeAppl("s457611")),None,SortAppl("MethodDecl", List())), CGenRecurse(TermVar("x457617"),List(ScopeAppl("s457618")),Some(TermVar("x457619")),SortAppl("Exp", List())), CGenRecurse(TermVar("x457616"),List(ScopeAppl("s457618")),None,SortAppl("IterStar", List(SortAppl("Statement", List())))), CGenRecurse(TermVar("x457615"),List(ScopeAppl("s457618")),None,SortAppl("IterStar", List(SortAppl("VarDecl", List())))), CGenRecurse(TermVar("x457614"),List(ScopeAppl("s457618")),Some(TermVar("x457620")),SortAppl("IterStar", List(SortAppl("ParamDecl", List())))), CSubtype(TermVar("x457619"),TermAppl("TClass", List(SymbolicName("Class", "x328829"))))),List(CGDecl(ScopeAppl("s408940"),SymbolicName("Class", "x328829")), CGAssoc(SymbolicName("Class", "x328829"),ScopeAppl("s457611")), CGDecl(ScopeAppl("s457611"),ConcreteName("Implicit", "this", 328835)), CGDirectEdge(ScopeAppl("s457611"),Label('P'),ScopeAppl("s408940")), CGDecl(ScopeAppl("s408940"),SymbolicName("Class", "x340230")), CGDecl(ScopeAppl("s457611"),SymbolicName("Method", "x457613")), CGRef(SymbolicName("Method", "x457613"),ScopeAppl("s457624")), CGDirectEdge(ScopeAppl("s457624"),Label('I'),ScopeAppl("s457611")), CGDirectEdge(ScopeAppl("s457624"),Label('S'),ScopeAppl("s457611")), CGDirectEdge(ScopeAppl("s457618"),Label('P'),ScopeAppl("s457611")), CGRef(SymbolicName("Class", "x457612"),ScopeAppl("s457611"))),TypeEnv(Map(Binding(SymbolicName("Class", "x328829"), TermAppl("TClass", List(SymbolicName("Class", "x328829")))), Binding(ConcreteName("Implicit", "this", 328835), TermAppl("TClass", List(SymbolicName("Class", "x328829")))), Binding(SymbolicName("Class", "x340230"), TermAppl("TMainClass", List())), Binding(SymbolicName("Method", "x457613"), TermAppl("TMethod", List(TermAppl("TClass", List(SymbolicName("Class", "x328829"))), TermVar("x457620")))))),Resolution(Map(Binding(SymbolicName("Method", "x457613"), SymbolicName("Method", "x457613")), Binding(SymbolicName("Class", "x457612"), SymbolicName("Class", "x328829")))),SubtypeRelation(List(Binding(TermAppl("TClass", List(SymbolicName("Class", "x328829"))), TermAppl("TClass", List(ConcreteName("Class", "Object", 351655))))))))
      val result = synergize(rules)(/*language.startRules.random*/start)

      if (result.isEmpty) {
        // Synergize returned None meaning there was an inconsistency..
        println("Failed")
      } else {
        // Print rule
        println(result)

        // Try to solve
        val solvedStates = Solver.solve(result.get.state)

        if (solvedStates.isEmpty) {
          println("Unsolvable")
        } else {
          // Print pretty-printed concrete solved rule
          val source = printer(
            Converter.toTerm(
              Concretor.concretize(result.get, solvedStates.random)
            )
          )

          // Append to results.log
          writer.println(source.stringValue())
          writer.println("===")
          writer.flush()

          // Print to stdout
          println(source.stringValue())
          println("===")
        }
      }
    }
  }

  // Expand rule with the best alternative from rules
  def synergize(rules: List[Rule])(current: Rule): Option[Rule] = {
    // Debug
    println(current)

    // Pick a random recurse constraint (position to expand the current rule)
    val recurseOpt = current.recurse.randomOption

    // If all recurse constraints are solved, return
    if (recurseOpt.isEmpty) {
      return Some(current)
    }

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = rules.flatMap(current.merge(recurseOpt.get, _, 2))

    // For every option, solve constraints and compute its score
    val scored = options.flatMap(score)

    if (scored.isEmpty) {
      return None
    }

    /*
    // Get the option with the best (lowest) score
    val best = scored.minBy(_._2)

    // Return the best
    best._1
    */

    // Take the top 10. We do not want to always take the best, as this is pretty deterministic.
    // We also don't want to take the top 5%, because then with 2 choices we get determinisitc as well.
    val bests = scored.sortBy(_._2).take(2)

    // If there is a complete fragment, return it!
    for ((rule, score) <- bests) if (score == 0) {
      return Some(rule)
    }

    // Backtracking
    /*
    for (best <- bests.shuffle) {
      val child = synergize(rules)(best._1)

      if (child.isDefined) {
        return child
      }
    }

    None
    */

    // Non-backtracking
    synergize(rules)(bests.random._1)
  }

  // Compute the solved rule and its score for each possible solution
  def score(rule: Rule): List[(Rule, Int)] = {
    // Compute the score for a single constraint
    def constraintScore(constraint: Constraint) = constraint match {
      case _: CResolve => 3
      case _: CGenRecurse => 1000
      case _ => 1
    }

    // Compute the score after crossing out constraints
    Consistency
      .solve(rule.state)
      .map(state => (rule.copy(state = state), state.constraints.map(constraintScore).sum))
  }

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule]): List[Rule] = {
    val ruleA = rules.random
    val ruleB = rules.random

    if (ruleA.recurse.nonEmpty) {
      val recurse = ruleA.recurse.random

      // TODO: we can already solve some constraints in the grown fragments, i.e. "cache" their solutions?

      ruleA
        .merge(recurse, ruleB, 1)
        .filter(_.pattern.size < 50)
        .map(_ :: rules)
        .getOrElse(rules)
    } else {
      rules
    }
  }

  // Repeat until f returns None or there are no more recurse constraints
  def repeatUntilNoneOrDone(f: Rule => Option[Rule], n: Int): Rule => Option[Rule] = {
    @tailrec def repeatAcc(acc: Rule, n: Int): Option[Rule] = n match {
      case 0 => Some(acc)
      case _ => f(acc) match {
        case Some(x) =>
          if (x.recurse.isEmpty) {
            Some(x)
          } else {
            repeatAcc(x, n - 1)
          }
        case None => None
      }
    }

    (t: Rule) => repeatAcc(t, n)
  }
}

// For parsing a printed rule
class Binding[A, B](a: A, b: B) extends Tuple2[A, B](a, b)

object Binding {
  def apply[A, B](a: A, b: B): Binding[A, B] =
    new Binding(a, b)
}
