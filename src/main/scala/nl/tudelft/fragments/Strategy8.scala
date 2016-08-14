package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

import scala.annotation.tailrec

object Strategy8 {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  implicit val signatures = Signatures.read(
    strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
    signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
  )

  implicit val rules = Specification.read(
    nablPath = "zip:/Users/martijn/Projects/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0-SNAPSHOT.spoofax-language!/",
    specPath = "/Users/martijn/Projects/scopes-frames/L3/trans/analysis/l3.nabl2"
  )

  def main(args: Array[String]): Unit = {
    val print = Printer.printer(
      languagePath = "/Users/martijn/Projects/scopes-frames/L3/"
    )

    val startRules = rules.filter(_.sort == SortAppl("Start"))

    // Randomly combine rules to build larger rules
    val base = repeat(grow, 200)(rules)

    // Start from a start rule and build a complete program
    for (i <- 0 to 100) {
      val result = build(startRules.random, base.shuffle, 100)
      println(result)
    }

//    val r = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s9591")), State(TermAppl("Program", List(TermVar("dd"), TermAppl("Seq", List(TermAppl("QVar", List(TermAppl("QVar", List(TermAppl("Assign", List(TermAppl("QVar", List(TermAppl("QVar", List(TermAppl("Assign", List(TermVar("x4307"), TermAppl("App", List(TermVar("x4308"), TermAppl("App", List(TermVar("x4309"), TermVar("x4310"))))))), TermVar("x3305"))), TermVar("x3306"))), TermAppl("Add", List(TermAppl("Add", List(TermVar("x9592"), TermVar("x9593"))), TermAppl("IntValue", List(TermVar("x1456"))))))), TermVar("x1457"))), TermVar("x1458"))), TermAppl("QVar", List(TermAppl("Var", List(TermVar("x1459"))), TermVar("x1460"))))))),List(CGenRecurse(TermVar("dd"),List(ScopeAppl("s9591")),None,SortAppl("List", List(SortAppl("Declaration", List())))), CAssoc(TermVar("x1461"),ScopeVar("s1462")), CResolve(SymbolicName("Var", "x1460"),TermVar("x1463")), CTypeOf(TermVar("x1463"),TermVar("x1464")), CResolve(SymbolicName("Var", "x1459"),TermVar("x1465")), CTypeOf(TermVar("x1465"),TermAppl("TClass", List(TermVar("x1461")))), CAssoc(TermVar("x1466"),ScopeVar("s1467")), CResolve(SymbolicName("Var", "x1458"),TermVar("x1468")), CTypeOf(TermVar("x1468"),TermVar("x1469")), CAssoc(TermVar("x1470"),ScopeVar("s1471")), CResolve(SymbolicName("Var", "x1457"),TermVar("x1472")), CTypeOf(TermVar("x1472"),TermAppl("TClass", List(TermVar("x1466")))), CSubtype(TermAppl("TInt", List()),TermAppl("TClass", List(TermVar("x1470")))), CTrue(), CAssoc(TermVar("x3307"),ScopeVar("s3308")), CResolve(SymbolicName("Var", "x3306"),TermVar("x3309")), CTypeOf(TermVar("x3309"),TermAppl("TClass", List(TermVar("x1470")))), CAssoc(TermVar("x3311"),ScopeVar("s3312")), CResolve(SymbolicName("Var", "x3305"),TermVar("x3313")), CTypeOf(TermVar("x3313"),TermAppl("TClass", List(TermVar("x3307")))), CGenRecurse(TermVar("x4307"),List(ScopeAppl("s9591")),Some(TermAppl("TClass", List(TermVar("x3311")))),SortAppl("Lhs", List())), CSubtype(TermVar("x4312"),TermAppl("TClass", List(TermVar("x3311")))), CGenRecurse(TermVar("x4308"),List(ScopeAppl("s9591")),Some(TermAppl("TFun", List(TermVar("x4313"), TermVar("x4312")))),SortAppl("Exp", List())), CSubtype(TermVar("x4314"),TermVar("x4313")), CGenRecurse(TermVar("x4310"),List(ScopeAppl("s9591")),Some(TermVar("x4315")),SortAppl("ResetExp", List())), CGenRecurse(TermVar("x4309"),List(ScopeAppl("s9591")),Some(TermAppl("TFun", List(TermVar("x4316"), TermVar("x4314")))),SortAppl("Exp", List())), CSubtype(TermVar("x4315"),TermVar("x4316")), CGenRecurse(TermVar("x9593"),List(ScopeAppl("s9591")),Some(TermAppl("TInt", List())),SortAppl("Exp", List())), CGenRecurse(TermVar("x9592"),List(ScopeAppl("s9591")),Some(TermAppl("TInt", List())),SortAppl("Exp", List()))),List(CGDirectEdge(ScopeAppl("s1473"),Label('I'),ScopeVar("s1462")), CGRef(SymbolicName("Var", "x1460"),ScopeAppl("s1473")), CGRef(SymbolicName("Var", "x1459"),ScopeAppl("s9591")), CGDirectEdge(ScopeAppl("s1474"),Label('I'),ScopeVar("s1467")), CGRef(SymbolicName("Var", "x1458"),ScopeAppl("s1474")), CGDirectEdge(ScopeAppl("s1475"),Label('I'),ScopeVar("s1471")), CGRef(SymbolicName("Var", "x1457"),ScopeAppl("s1475")), CGDirectEdge(ScopeAppl("s3314"),Label('I'),ScopeVar("s3308")), CGRef(SymbolicName("Var", "x3306"),ScopeAppl("s3314")), CGDirectEdge(ScopeAppl("s3315"),Label('I'),ScopeVar("s3312")), CGRef(SymbolicName("Var", "x3305"),ScopeAppl("s3315"))),TypeEnv(),Resolution(),SubtypeRelation(),List()))
//    for (resolve <- r.resolutionConstraints) {
//      fix(r, resolve, base)
//    }
  }

  // Randomly merge rules in a rules into larger consistent rules
  def grow(rules: List[Rule])(implicit signatures: List[Signatures.Decl]): List[Rule] = {
    val ruleA = rules.random
    val ruleB = rules.random

    if (ruleA.recurse.nonEmpty) {
      val recurse = ruleA.recurse.random

      // TODO: During 'grow' we should not check consistency of resolve constraints
      ruleA
        .merge(recurse, ruleB)
        .map(_ :: rules)
        .getOrElse(rules)
    } else {
      rules
    }
  }

  // Build a complete program by growing a partial program
  def build(partial: Rule, rules: List[Rule], fuel: Int)(implicit signatures: List[Signatures.Decl]): Either[Rule, Int] = {
    println(fuel)
//    println(partial)

    if (partial.recurse.isEmpty) {
      println("Complete program: " + partial)

      if (Solver.solve(partial.state).nonEmpty) {
        println("Solved!")
      } else {
        println("Unable to solve..")
      }

      Left(partial)
    } else {
      val recurse = partial.recurse.random

      val maxSize = 20
      val remSize = maxSize - partial.pattern.size
      val divSize = remSize / partial.recurse.size

      // Testing something..
      if (divSize > 2) {
        val mergedRules = for {rule <- rules.shuffle if rule.pattern.size <= divSize} yield {
          partial.merge(recurse, rule)
        }

        var remainingFuel = fuel

        for (mergedRule <- mergedRules.flatten) {
          remainingFuel = remainingFuel-1

          val complete = build(mergedRule, rules, remainingFuel)

          if (complete.isLeft) {
            return complete
          } else {
            remainingFuel = complete.asInstanceOf[Right[_, Int]].b

            if (remainingFuel < 0) {
              println("Out of fuel")

              return Right(remainingFuel)
            }
          }
        }

        Right(remainingFuel)
      } else {
        Right(fuel)
      }
    }
  }

  // Fix a resolution constraint by merging with a fragment that provides a declaration
  def fix(partial: Rule, resolve: CResolve, rules: List[Rule])(implicit signatures: List[Signatures.Decl]): Option[Rule] = {
    println(partial)

    // 1. Find all scopes that are reachable from the reference
    val graph = Graph(partial.state.facts)
    val scope = graph.scope(resolve.n1).get
    val reachable = graph.reachableScopes(partial.state.resolution)(scope)

    // NOTE: reachable does not include ScopeVars

    // 2. Find all recurse constraints that receive the scope as a parameter
    val applicableRecurses = partial.recurse
      .filter(_.scopes intersect reachable nonEmpty)

    // 3. Find all fragments that will make resolution possible after merging
    val results = (for (applicableRecurse <- applicableRecurses) yield {
      val mergedRules = rules.flatMap(partial.merge(applicableRecurse, _))

      mergedRules.filter(mergedRule => {
        Graph(mergedRule.state.facts).res(mergedRule.state.resolution)(resolve.n1).nonEmpty
      })
    }).flatten

    // 4. Pick one of these
    if (results.nonEmpty) {
      Some(results.random)
    } else {
      None
    }
  }

  // Returns a function x => f(f(f(x))) with n times f
  def repeat[T](f: T => T, n: Int): T => T = {
    @tailrec def repeatAcc(acc: T, n: Int): T = n match {
      case 0 => acc
      case _ => repeatAcc(f(acc), n - 1)
    }

    (t: T) => repeatAcc(t, n)
  }
}
