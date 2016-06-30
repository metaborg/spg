package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures._
import org.scalatest.FunSuite

class RuleSuite extends FunSuite {
  test("merge") {
    implicit val signatures = List(
      OpDeclInj(FunType(List(ConstType(SortAppl("Exp"))),ConstType(SortAppl("Start")))),
      OpDecl("IntValue",FunType(List(ConstType(SortAppl("INT"))),ConstType(SortAppl("Exp")))),
      OpDecl("Var",FunType(List(ConstType(SortAppl("ID"))),ConstType(SortAppl("Exp")))),
      OpDecl("Add",FunType(List(ConstType(SortAppl("Exp")), ConstType(SortAppl("Exp"))),ConstType(SortAppl("Exp")))),
      OpDecl("Fun",FunType(List(ConstType(SortAppl("ID")), ConstType(SortAppl("Type")), ConstType(SortAppl("Exp"))),ConstType(SortAppl("Exp")))),
      OpDecl("App",FunType(List(ConstType(SortAppl("Exp")), ConstType(SortAppl("ResetExp"))),ConstType(SortAppl("Exp")))),
      OpDeclInj(FunType(List(ConstType(SortAppl("Exp"))),ConstType(SortAppl("ResetExp")))),
      OpDecl("IntType",ConstType(SortAppl("Type"))),
      OpDecl("FunType",FunType(List(ConstType(SortAppl("Type")), ConstType(SortAppl("Type"))),ConstType(SortAppl("Type"))))
    )

    val rule = Rule(SortAppl("Exp", List()), Some(TypeAppl("TFun", List(TypeAppl("TInt", List()), TypeVar("t15")))), List(ScopeVar("s")), State(TermAppl("Fun", List(TermVar("x55791"))), List(CTypeOf(SymbolicName("Var", "x"), TypeAppl("TFun", List(TypeVar("t55788"), TypeVar("t55789")))), CResolve(SymbolicName("Var", "x658"),NameVar("d659")), CTypeOf(NameVar("d659"),TypeAppl("TFun", List(TypeVar("t93"), TypeAppl("TFun", List(TypeVar("t19"), TypeVar("t15")))))), CGenRecurse(TermVar("x55791"),List(),Some(TypeVar("t55789")),SortAppl("Type", List())), CGenRecurse(TermVar("x55790"),List(),Some(TypeVar("t55788")),SortAppl("Type", List())), CGenRecurse(TermVar("x55887"),List(),Some(TypeVar("t55885")),SortAppl("Type", List())), CGenRecurse(TermVar("x55886"),List(),Some(TypeVar("t55884")),SortAppl("Type", List())), CGenRecurse(TermVar("x56245"),List(),Some(TypeVar("t56243")),SortAppl("Type", List())), CGenRecurse(TermVar("x56244"),List(),Some(TypeVar("t56242")),SortAppl("Type", List()))),List(CGDecl(ScopeVar("s575"),SymbolicName("Var", "x")), CGDirectEdge(ScopeVar("s575"),Label('P'),ScopeVar("s")), CGDecl(ScopeVar("s657"),SymbolicName("Var", "x576")), CGDirectEdge(ScopeVar("s657"),Label('P'),ScopeVar("s575")), CGRef(SymbolicName("Var", "x658"),ScopeVar("s657"))),TypeEnv(),Resolution(),SubtypeRelation(),List()))
    val recurse = CGenRecurse(TermVar("x55791"),List(),Some(TypeVar("t55789")),SortAppl("Type", List()))
    val other = Rule(SortAppl("Type", List()), Some(TypeAppl("TFun", List(TypeVar("t1'"), TypeVar("t2'")))), List(), State(TermAppl("FunType", List(TermVar("t1"), TermVar("t2"))),List(CGenRecurse(TermVar("t2"),List(),Some(TypeVar("t2'")),SortAppl("Type", List())), CGenRecurse(TermVar("t1"),List(),Some(TypeVar("t1'")),SortAppl("Type", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(),List()))
    val merged = rule.merge(recurse, other)

    println(merged)
  }

  test("merge with scopes") {
    implicit val signatures = Nil

    val r1 = Rule(SortAppl("Start", List()), None, List(ScopeAppl("s")), State(TermAppl("Program", List(TermVar("dd"), TermVar("e"))),List(CGenRecurse(TermVar("e"),List(ScopeAppl("s")),Some(TypeVar("t")),SortAppl("Exp", List())), CGenRecurse(TermVar("dd"),List(ScopeAppl("s")),None,SortAppl("List", List(SortAppl("Declaration", List()))))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
    val r2 = Rule(SortAppl("Exp", List()), Some(TypeAppl("TInt", List())), List(ScopeAppl("s11")), State(TermAppl("IntValue", List(TermVar("x10"))),List(CTrue()),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
    val recurse = CGenRecurse(TermVar("e"),List(ScopeAppl("s")),Some(TypeVar("t")),SortAppl("Exp", List()))

    assert(r1.merge(recurse, r2).isDefined)
  }

  test("unify concrete scopes") {
    val s1 = ScopeAppl("s1")
    val s2 = ScopeAppl("s2")

    assert(s1.unify(s2) == Some(Map(s1 -> s2)))
  }
}
