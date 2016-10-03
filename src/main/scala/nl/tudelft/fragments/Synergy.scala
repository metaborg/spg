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
  val termSizeLimit = 200

  def main(args: Array[String]): Unit = {
    // Grow some programs
    logger.info("Start growing")

    val rules = repeat(grow, steps)(Synergy.rules)

    // Synergize from a random start rule
    logger.info("Start synergizing with " + rules.length + " rules")

    val writer = new PrintWriter(new FileOutputStream(new File("results.log"), true))

    for (i <- 0 to 1000) {
      // This produced a call:
      val start = Rule(SortAppl("Program", List()), None, List(ScopeAppl("s118361")), State(TermAppl("Program", List(TermVar("mc"), TermAppl("Cons", List(TermVar("x118362"), TermVar("x118363"))))),List(CGenRecurse(TermVar("mc"),List(ScopeAppl("s118361")),None,SortAppl("MainClass", List())), CGenRecurse(TermVar("x118363"),List(ScopeAppl("s118361")),None,SortAppl("List", List(SortAppl("ClassDecl", List())))), CGenRecurse(TermVar("x118362"),List(ScopeAppl("s118361")),None,SortAppl("ClassDecl", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(List())))

      //val start = Rule(SortAppl("Program", List()), None, List(ScopeAppl("s1251080")), State(TermAppl("Program", List(TermAppl("MainClass", List(TermVar("x195685"), TermVar("x195686"), TermAppl("Print", List(TermAppl("Int", List(TermVar("x253777"))))))), TermAppl("Cons", List(TermAppl("Class", List(TermVar("x274956"), TermAppl("NoParent", List()), TermAppl("Nil", List()), TermAppl("Cons", List(TermAppl("Method", List(TermAppl("ClassType", List(TermVar("x1274022"))), TermVar("x1266366"), TermAppl("Cons", List(TermAppl("Param", List(TermAppl("ClassType", List(TermVar("x22922"))), TermVar("x1277456"))), TermAppl("Nil", List()))), TermAppl("Nil", List()), TermAppl("Cons", List(TermAppl("Block", List(TermAppl("Nil", List()))), TermAppl("Nil", List()))), TermAppl("Call", List(TermVar("x41605"), TermVar("x41606"), TermVar("x41607"))))), TermAppl("Nil", List()))))), TermAppl("Nil", List()))))),List(CSubtype(TermVar("x41612"),TermAppl("TClass", List(SymbolicName("Class", "x274956")))), CGenRecurse(TermVar("x41607"),List(ScopeAppl("s22921")),Some(TermVar("x41608")),SortAppl("IterStar", List(SortAppl("Exp", List())))), CGenRecurse(TermVar("x41605"),List(ScopeAppl("s22921")),Some(TermAppl("TClass", List(TermVar("x41609")))),SortAppl("Exp", List())), CAssoc(TermVar("x41609"),ScopeVar("s41610")), CResolve(SymbolicName("Method", "x41606"),TermVar("x41611")), CTypeOf(TermVar("x41611"),TermAppl("TMethod", List(TermVar("x41612"), TermVar("x41613")))), CSubtype(TermVar("x41608"),TermVar("x41613"))),List(CGDecl(ScopeAppl("s1251080"),SymbolicName("Class", "x195685")), CGDecl(ScopeAppl("s1251080"),SymbolicName("Class", "x274956")), CGAssoc(SymbolicName("Class", "x274956"),ScopeAppl("s1274021")), CGDecl(ScopeAppl("s1274021"),ConcreteName("Implicit", "this", 274962)), CGDirectEdge(ScopeAppl("s1274021"),Label('P'),ScopeAppl("s1251080")), CGDecl(ScopeAppl("s1274021"),SymbolicName("Method", "x1266366")), CGDirectEdge(ScopeAppl("s22921"),Label('P'),ScopeAppl("s1274021")), CGRef(SymbolicName("Class", "x1274022"),ScopeAppl("s1274021")), CGDecl(ScopeAppl("s22921"),SymbolicName("Var", "x1277456")), CGDirectEdge(ScopeAppl("s41614"),Label('I'),ScopeVar("s41610")), CGRef(SymbolicName("Method", "x41606"),ScopeAppl("s41614")), CGRef(SymbolicName("Class", "x22922"),ScopeAppl("s22921"))),TypeEnv(Map(Binding(SymbolicName("Class", "x274956"), TermAppl("TClass", List(SymbolicName("Class", "x274956")))), Binding(SymbolicName("Var", "x1277456"), TermAppl("TClass", List(SymbolicName("Class", "x274956")))), Binding(ConcreteName("Implicit", "this", 274962), TermAppl("TClass", List(SymbolicName("Class", "x274956")))), Binding(SymbolicName("Class", "x195685"), TermAppl("TMainClass", List())), Binding(SymbolicName("Method", "x1266366"), TermAppl("TMethod", List(TermAppl("TClass", List(SymbolicName("Class", "x274956"))), TermAppl("Cons", List(TermAppl("TClass", List(SymbolicName("Class", "x274956"))), TermAppl("Nil", List())))))))),Resolution(Map(Tuple2(SymbolicName("Class", "x1274022"), SymbolicName("Class", "x274956")), Tuple2(SymbolicName("Class", "x22922"), SymbolicName("Class", "x274956")))),SubtypeRelation(List(Binding(TermAppl("TClass", List(SymbolicName("Class", "x274956"))), TermAppl("TClass", List(ConcreteName("Class", "Object", 1251083))))))))

      val result = synergize(rules)(/*language.startRules.random*/start)

      if (result.isEmpty) {
        // Synergize returned None meaning there was an inconsistency or the term diverged
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
          writer.println("===")
          writer.println(result)
          writer.println("---")
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

    // Prevent diverging
    if (current.pattern.size > termSizeLimit) {
      return None
    }

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
