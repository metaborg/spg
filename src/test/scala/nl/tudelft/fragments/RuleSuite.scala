package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures.{ConstType, FunType, OpDecl, OpDeclInj}
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

    val rule = Rule(SortAppl("Exp", List()), TypeAppl("TFun", List(TypeAppl("TInt", List()), TypeVar("t15"))), List(ScopeVar("s")), State(TermAppl("Fun", List(TermVar("x55791"))), List(TypeOf(SymbolicName("Var", "x"), TypeAppl("TFun", List(TypeVar("t55788"), TypeVar("t55789")))), Res(SymbolicName("Var", "x658"),NameVar("d659")), TypeOf(NameVar("d659"),TypeAppl("TFun", List(TypeVar("t93"), TypeAppl("TFun", List(TypeVar("t19"), TypeVar("t15")))))), Recurse(TermVar("x55791"),List(),TypeVar("t55789"),SortAppl("Type", List())), Recurse(TermVar("x55790"),List(),TypeVar("t55788"),SortAppl("Type", List())), Recurse(TermVar("x55887"),List(),TypeVar("t55885"),SortAppl("Type", List())), Recurse(TermVar("x55886"),List(),TypeVar("t55884"),SortAppl("Type", List())), Recurse(TermVar("x56245"),List(),TypeVar("t56243"),SortAppl("Type", List())), Recurse(TermVar("x56244"),List(),TypeVar("t56242"),SortAppl("Type", List()))),List(Dec(ScopeVar("s575"),SymbolicName("Var", "x")), Par(ScopeVar("s575"),ScopeVar("s")), Dec(ScopeVar("s657"),SymbolicName("Var", "x576")), Par(ScopeVar("s657"),ScopeVar("s575")), Ref(SymbolicName("Var", "x658"),ScopeVar("s657"))),TypeEnv(),Resolution(),List()))
    val recurse = Recurse(TermVar("x55791"),List(),TypeVar("t55789"),SortAppl("Type", List()))
    val other = Rule(SortAppl("Type", List()), TypeAppl("TFun", List(TypeVar("t1'"), TypeVar("t2'"))), List(), State(TermAppl("FunType", List(TermVar("t1"), TermVar("t2"))),List(Recurse(TermVar("t2"),List(),TypeVar("t2'"),SortAppl("Type", List())), Recurse(TermVar("t1"),List(),TypeVar("t1'"),SortAppl("Type", List()))),List(),TypeEnv(),Resolution(),List()))
    val merged = rule.merge(recurse, other)

    println(merged)
  }
}
