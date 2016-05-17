package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.{Converter, Printer}
import org.scalatest.FunSuite

class ConcretorSuite extends FunSuite {
  test("concretize bug") {
    val r = Rule(TermAppl("Program", List(TermAppl("MainClass", List(PatternNameAdapter(SymbolicName("n214082")), PatternNameAdapter(SymbolicName("n214083")), TermAppl("Block", List(TermAppl("Nil", List()))))), TermAppl("Cons", List(TermAppl("Class", List(PatternNameAdapter(SymbolicName("n214037")), TermAppl("Parent", List(PatternNameAdapter(SymbolicName("n214070")))), TermAppl("Nil", List()), TermAppl("Cons", List(TermAppl("Method", List(TermAppl("Bool", List()), PatternNameAdapter(SymbolicName("n214045")), TermAppl("Cons", List(TermAppl("Param", List(TermAppl("IntArray", List()), PatternNameAdapter(SymbolicName("n214098")))), TermAppl("Cons", List(TermAppl("Param", List(TermAppl("ClassType", List(PatternNameAdapter(SymbolicName("n214209")))), PatternNameAdapter(SymbolicName("n214079")))), TermAppl("Nil", List()))))), TermAppl("Nil", List()), TermAppl("Nil", List()), TermAppl("True", List()))), TermAppl("Nil", List()))))), TermAppl("Nil", List()))))), SortAppl("Program", List()), TypeVar("t214205"), ScopeVar("s214206"), List(Ref(SymbolicName("n214209"),ScopeVar("s214211")), Res(SymbolicName("n214209"),NameVar("d214212")), TypeOf(NameVar("d214212"),TypeVar("t214210")), TypeEquals(TypeVar("t214210"),TypeAppl("IntArray", List())), TypeEquals(TypeVar("t214197"),TypeAppl("Bool", List())), Dec(ScopeVar("s214211"),SymbolicName("n214098")), TypeOf(SymbolicName("n214098"),TypeVar("t214210")), Dec(ScopeVar("s214211"),SymbolicName("n214079")), TypeOf(SymbolicName("n214079"),TypeVar("t214210")), Ref(SymbolicName("n214070"),ScopeVar("s214206")), Res(SymbolicName("n214070"),NameVar("d214073")), TypeEquals(TypeVar("t214197"),TypeAppl("Bool", List())), Dec(ScopeVar("s214081"),SymbolicName("n214045")), Par(ScopeVar("s214211"),ScopeVar("s214081")), AssocFact(SymbolicName("n214045"),ScopeVar("s214211")), TypeOf(SymbolicName("n214045"),TypeAppl("Fun", List(TypeVar("t214197"), TypeVar("t214057")))), Dec(ScopeVar("s214206"),SymbolicName("n214037")), TypeOf(SymbolicName("n214037"),TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("n214037"))))), Par(ScopeVar("s214081"),ScopeVar("s214206")), AssocFact(NameVar("n214037"),ScopeVar("s214081"))))
    val s = Solver.solve(r.constraints)

    if (s.isDefined) {
      println(s.get)

      val concretePattern = Concretor.concretize(r, s.get)
      val strategoTerm = Converter.toTerm(concretePattern)
      val text = Printer.print("/Users/martijn/Documents/workspace/MiniJava")(strategoTerm).stringValue()

      println(text)
    }
  }
}
