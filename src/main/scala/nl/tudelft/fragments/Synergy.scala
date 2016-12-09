package nl.tudelft.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.Logger
import nl.tudelft.fragments.spoofax.models.{SortAppl, SortVar}
import nl.tudelft.fragments.spoofax.{Converter, Language}
import org.slf4j.LoggerFactory

import scala.util.Random

object Synergy {
  val logger = Logger(LoggerFactory.getLogger(this.getClass))
  val verbose = false
  val interactive = true

  // Make the language parts implicitly available
  //implicit val language = Language.load("/Users/martijn/Projects/scopes-frames/L3", "org.metaborg:L3:0.1.0-SNAPSHOT", "L3")
  implicit val language = Language.load("/Users/martijn/Projects/metaborg-tiger/org.metaborg.lang.tiger", "org.metaborg:org.metaborg.lang.tiger:0.1.0-SNAPSHOT", "Tiger")
  //implicit val language = Language.load("/Users/martijn/Projects/MiniJava", "org.metaborg:MiniJava:0.1.0-SNAPSHOT", "MiniJava")
  implicit val productions = language.productions
  implicit val signatures = language.signatures
  implicit val specification = language.specification
  implicit val printer = language.printer
  implicit val rules = specification.rules

  def statistics(states: List[State])(implicit language: Language): Unit = {
    // Count the number of occurrences of the given constructor in the states
    val collect = (cons: String) => states.flatMap(_.pattern.collect {
      case x@TermAppl(`cons`, _) =>
        List(x)
      case _ =>
        Nil
    })

    // Term size
    val termSizes = states.map(state => state.pattern.size)

    // Number of name resolutions
    val resolutionCounts = states.map(state => state.resolution.size)

    // Count constructor occurrences
    println("# Statistics")

    println("Generated terms = " + states.size)
    println("Average term size = " + termSizes.average)
    println("Average resolution count = " + resolutionCounts.average)

    // Constructor counts
    println("## Constructor count (average)")

    for (constructor <- language.constructors) {
      println(s"'$constructor' constructors = " + collect(constructor).size / states.size.toFloat)
    }

    // Distribution of terms
    println("## Distribution of terms")

    val grouped = states.groupByWith(_.pattern, Pattern.equivalence).toList
      // Bind the size of the group
      .map {
        case (pattern, states) =>
          (states.size, pattern, states)
      }
      // Sort descending by size
      .sortBy {
        case (size, pattern, states) =>
          -size
      }

    grouped.foreach { case (size, pattern, states) =>
      println(s"${size}x $pattern")

      /*
      for (state <- states) {
        println(s"-- $state")
      }
      */
    }

    println(s"${grouped.size} groups")
  }

  def main(args: Array[String]): Unit = {
    val rules = Synergy.rules
    val writer = new PrintWriter(new FileOutputStream(new File("results.log"), true))

    val states = (0 to 20000).toList.flatMap(i => {
      //bench()
      println(i)

      // Pick a random start rule
      val startRule = language.startRules.random

      // First apply the init generation rule
      val init = language.specification.init

      // Merge the artificial `x` pattern in the init rule with a random start rule
      //val start = init.merge(CGenRecurse("Default", init.state.pattern, init.scopes, init.typ, startRule.sort), startRule, 0).get
      //val start = Rule("Init", SortVar("y"), Some(Var("x613")), List(ScopeAppl("s349")), State(TermAppl("Mod", List(TermAppl("Let", List(TermAppl("Cons", List(TermAppl("TypeDecs", List(TermAppl("Conss", List(Var("x386"), Var("x387"))))), TermAppl("Nil", List()))), TermAppl("Cons", List(TermAppl("Record", List(Var("x610"), TermAppl("Cons", List(Var("x770"), Var("x771"))))), TermAppl("Nil", List()))))))),List(CDistinct(Declarations(ScopeAppl("s769"), "All")), CGenRecurse("Map", Var("x387"), List(ScopeAppl("s769")), None, SortAppl("List", List(SortAppl("TypeDec", List())))), CGenRecurse("Default", Var("x386"), List(ScopeAppl("s769")), None, SortAppl("TypeDec", List())), CGenRecurse("Default", Var("x610"), List(ScopeAppl("s769")), Some(TermAppl("RECORD", List(Var("x614")))), SortAppl("TypeId", List())), CGenRecurse("Map2", Var("x771"), List(ScopeAppl("s768"), ScopeAppl("s769")), None, SortAppl("List", List(SortAppl("InitField", List())))), CGenRecurse("Default", Var("x770"), List(ScopeAppl("s768"), ScopeAppl("s769")), None, SortAppl("InitField", List()))),List(CGDecl(ScopeAppl("s349"),ConcreteName("Type", "int", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Type", "string", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "print", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "flush", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "getchar", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "ord", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "chr", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "size", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "substring", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "concat", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "not", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "exit", -1)), CGDirectEdge(ScopeAppl("s769"),Label('P'),ScopeAppl("s349")), CGDirectEdge(ScopeAppl("s768"),Label('I'),ScopeVar("x614"))),TypeEnv(Map(Binding(ConcreteName("Var", "concat", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "size", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "print", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "ord", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "chr", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("STRING", List())))), Binding(ConcreteName("Type", "string", -1), TermAppl("STRING", List())), Binding(ConcreteName("Var", "substring", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "not", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "exit", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "getchar", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "flush", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("UNIT", List())))), Binding(ConcreteName("Type", "int", -1), TermAppl("INT", List())))),Resolution(Map()),SubtypeRelation(List()),List()))
      val start = Rule("Init", SortVar("y"), Some(Var("x613")), List(ScopeAppl("s349")), State(TermAppl("Mod", List(TermAppl("Let", List(TermAppl("Cons", List(TermAppl("TypeDecs", List(TermAppl("Conss", List(TermAppl("TypeDec", List(Var("x286"), Var("x287"))), TermAppl("Nil", List()))))), TermAppl("Nil", List()))), TermAppl("Cons", List(TermAppl("Record", List(TermAppl("Tid", List(Var("x484"))), TermAppl("Cons", List(Var("x770"), TermAppl("Nil", List()))))), TermAppl("Nil", List()))))))),List(CDistinct(Declarations(ScopeAppl("s101"), "All")), CGenRecurse("Default", Var("x770"), List(ScopeAppl("s101"), ScopeAppl("s101")), None, SortAppl("InitField", List())), CGenRecurse("Default", Var("x287"), List(ScopeAppl("s101")), Some(TermAppl("RECORD", List(Var("x614")))), SortAppl("Type", List()))),List(CGDecl(ScopeAppl("s349"),ConcreteName("Type", "int", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Type", "string", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "print", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "flush", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "getchar", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "ord", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "chr", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "size", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "substring", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "concat", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "not", -1)), CGDecl(ScopeAppl("s349"),ConcreteName("Var", "exit", -1)), CGDirectEdge(ScopeAppl("s101"),Label('P'),ScopeAppl("s349")), CGDirectEdge(ScopeAppl("s101"),Label('I'),ScopeVar("x614")), CGDecl(ScopeAppl("s101"),SymbolicName("Type", "x286")), CGRef(SymbolicName("Type", "x484"),ScopeAppl("s101"))),TypeEnv(Map(Binding(ConcreteName("Var", "concat", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "size", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "print", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(SymbolicName("Type", "x286"), TermAppl("RECORD", List(Var("x614")))), Binding(ConcreteName("Var", "ord", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "chr", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("STRING", List())))), Binding(ConcreteName("Type", "string", -1), TermAppl("STRING", List())), Binding(ConcreteName("Var", "substring", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Cons", List(TermAppl("STRING", List()), TermAppl("Nil", List()))))))), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "not", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("INT", List())))), Binding(ConcreteName("Var", "exit", -1), TermAppl("FUN", List(TermAppl("Cons", List(TermAppl("INT", List()), TermAppl("Nil", List()))), TermAppl("UNIT", List())))), Binding(ConcreteName("Var", "getchar", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("STRING", List())))), Binding(ConcreteName("Var", "flush", -1), TermAppl("FUN", List(TermAppl("Nil", List()), TermAppl("UNIT", List())))), Binding(ConcreteName("Type", "int", -1), TermAppl("INT", List())))),Resolution(Map(Tuple2(SymbolicName("Type", "x484"), SymbolicName("Type", "x286")))),SubtypeRelation(List()),List()))

      // Generate!
      val result = synergize(rules)(start)

      result match {
        case None => {
          // Synergize returned None meaning there was an inconsistency or the term diverged
          if (verbose) {
            println("Failed")
          }

          None
        }
        case Some(rule) => {
          // Print rule
          if (verbose) {
            println(result)
          }

          // Solve remaining constraints
          val solvedStates = Solver.solve(rule.state)

          if (solvedStates.isEmpty) {
            if (verbose) {
              println("Unsolvable")
            }

            None
          } else {
            // State
            val state = solvedStates.random

            // Concretize the tree
            val concrete = Concretor(language).concretize(rule, state)

            // Convert back to IStrategoTerm
            val term = Converter.toTerm(concrete)

            // Pretty-print IStrategoTerm to String
            val source = printer(term)

            // Print to stdout
            if (verbose) {
              println(source)
            }

            // Append to results.log
            writer.println("===============")
            writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
            writer.println("---------------")
            writer.println(source)
            writer.flush()

            //bench()

            Some(state)
          }
        }
      }
    })

    statistics(states)
  }

  // Expand rule with the best alternative from rules
  def synergize(rules: List[Rule])(term: Rule): Option[Rule] = {
    if (verbose) {
      println(term)
    }

    // Prevent diverging
    if (term.pattern.size > 40) {
      return None
    }

    // If the term is complete, return
    if (term.recurse.isEmpty) {
      return Some(term)
    }

    // Pick a random recurse constraint (position to expand the current rule)
    val recurse = term.recurse.random

    // Merge with every other rule and filter out inconsistent merges and too big rules
    val options = rules.flatMap(term.merge(recurse, _, 2))

    // For every option, solve constraints and compute its score
    val scored = options.flatMap(score)

    if (scored.isEmpty) {
      return None
    }

    // Take the best n programs. We shuffle before sorting to randomize ruels with same score
    val bests = scored.shuffle.sortBy(_._2).take(15)

    // If there is a complete fragment, return it with p = 0.2
    for ((rule, score) <- bests) if (score == 0 && Random.nextInt(5) == 0) {
      return Some(rule)
    }

    // Approach A: Backtrack among the best programs
    /*
    for (best <- bests) {
      synergize(rules)(bests.random._1) match {
        case r@Some(_) => return r
        case _ =>
      }
    }

    None
    */

    // Approach B: Continue with a random program among the best programs
    if (interactive) {
      println(s"Expand hole ${recurse.pattern}: $recurse")
      println("Which term would you like to continue with?")
      for (((rule, score), index) <- scored.zipWithIndex) {
        println(s"[$index]: Score: $score, Rule: $rule")
      }

      val choice = scala.io.StdIn.readInt()

      synergize(rules)(scored(choice)._1)
    } else {
      synergize(rules)(bests.random._1)
    }
  }

  // Compute the solved rule and its score for each possible solution
  def score(rule: Rule): List[(Rule, Int)] = {
    // Compute the score for a single constraint
    def constraintScore(constraint: Constraint) = constraint match {
      case _: CResolve => 3
      case _: CGenRecurse => 6
      case _: CTrue => 0
      case _ => 1
    }

    // Compute the score after eliminating constraints
    Eliminate
      .solve(rule.state)
      .map(state => (rule.withState(state), state.constraints.map(constraintScore).sum))
      .filter { case (rule, _) => Consistency.check(rule) }
  }

  // Print the time so we can bencmark
  def bench() = {
    val now = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now())
    println("=== " + now)
  }
}

// For parsing a printed rule
class Binding[A, B](a: A, b: B) extends Tuple2[A, B](a, b)

object Binding {
  def apply[A, B](a: A, b: B): Binding[A, B] =
    new Binding(a, b)
}
