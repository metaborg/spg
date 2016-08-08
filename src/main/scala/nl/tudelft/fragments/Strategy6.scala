package nl.tudelft.fragments

import javax.inject.Singleton

import nl.tudelft.fragments.spoofax.{Printer, Signatures, Specification}
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

// Build programs top-down, keeping them consistent at every step. This strategy fails on QVar, since you cannot achieve consistency!
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

    val r = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s2542")), State(TermAppl("Program", List(TermAppl("Cons", List(TermAppl("Class", List(Var("x2543"), TermAppl("None", List()), TermAppl("Cons", List(TermAppl("Field", List(Var("x3414"), TermAppl("ClassType", List(Var("x4047"))), TermAppl("NewObject", List(Var("x3481"))))), TermAppl("Nil", List()))))), TermAppl("Nil", List()))), TermAppl("IntValue", List(Var("x19"))))),List(CTrue(), CTrue(), CTypeOf(SymbolicName("Class", "x2543"),TermAppl("TClassDef", List(SymbolicName("Class", "x2543")))), CTrue(), CTypeOf(SymbolicName("Var", "x3414"),TermAppl("TClass", List(Var("d4048")))), CSubtype(TermAppl("TClass", List(Var("d3482"))),TermAppl("TClass", List(Var("d4048")))), CResolve(SymbolicName("Class", "x3481"),Var("d3482")), CResolve(SymbolicName("Class", "x4047"),Var("d4048"))),List(CGDecl(ScopeAppl("s2542"),SymbolicName("Class", "x2543")), CGAssoc(SymbolicName("Class", "x2543"),ScopeAppl("s4046")), CGDirectEdge(ScopeAppl("s4046"),Label('P'),ScopeAppl("s2542")), CGDecl(ScopeAppl("s4046"),SymbolicName("Var", "x3414")), CGRef(SymbolicName("Class", "x3481"),ScopeAppl("s4046")), CGRef(SymbolicName("Class", "x4047"),ScopeAppl("s4046"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
    println(Solver.solve(r.state))

    build(startRules.random)
  }

  def build(partial: Rule)(implicit rules: List[Rule], signatures: List[Signatures.Decl]): List[Rule] = {
    if (partial.pattern.size > 10) {
      None
    } else {
      if (partial.recurse.isEmpty) {
        println("Complete program: " + partial)

        val states = Solver.solve(partial.state)

        if (states.isEmpty) {
          println("Could not solve it..")

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
                println("Consistent but not solvable: " + rule)
              }
            }

            rule
          })
          .flatMap(build)
      }
    }
  }
}
