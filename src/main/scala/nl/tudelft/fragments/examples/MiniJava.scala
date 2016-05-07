package nl.tudelft.fragments.examples

import nl.tudelft.fragments.{Dec, NameVar, Par, Ref, Res, Rule, ScopeVar, SortAppl, SortVar, TermAppl, TermVar, TypeAppl, TypeEquals, TypeOf, TypeVar}

object MiniJava {
  // Program : MainClass * List(ClassDecl) -> Program
  private val ruleProgram = Rule(
    TermAppl("Program", List(
      TermVar("x1", SortAppl("MainClass"), TypeVar("t"), ScopeVar("s")),
      TermVar("x2", SortAppl("List", List(SortAppl("Class"))), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("Program"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // MainClass : ID * ID * Statement -> MainClass
  private val ruleMainClass = Rule(
    TermAppl("MainClass", List(
      NameVar("n1"),
      NameVar("n2"),
      TermVar("x", SortAppl("Statement"), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("MainClass"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // Class : ID * ParentDecl * List(FieldDecl) * List(MethodDecl) -> ClassDecl
  private val ruleClass = Rule(
    TermAppl("Class", List(
      NameVar("n"),
      TermVar("x1", SortAppl("ParentDecl"), TypeVar("t"), ScopeVar("s")),
      TermVar("x2", SortAppl("List", List(SortAppl("FieldDecl"))), TypeVar("t"), ScopeVar("s")),
      TermVar("x3", SortAppl("List", List(SortAppl("Method"))), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("Class"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // Parent : ID -> ParentDecl
  private val ruleParent = Rule(
    TermAppl("Parent", List(
      NameVar("n")
    )),
    SortAppl("ParentDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s"))
    )
  )

  // None : ParentDecl
  private val ruleNone = Rule(
    TermAppl("None"),
    SortAppl("ParentDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // Method : Type * ID * List(ParamDecl) * List(VarDecl) * List(Statement) * Exp -> MethodDecl (TODO: Type of list of ParamDecl goes wrong)
  private val ruleMethod = Rule(
    TermAppl("Method", List(
      TermVar("x1", SortAppl("Type"), TypeVar("t2"), ScopeVar("s")),
      NameVar("n1"),
      TermVar("x2", SortAppl("List", List(SortAppl("ParamDecl"))), TypeVar("t3"), ScopeVar("s1")),
      TermVar("x3", SortAppl("List", List(SortAppl("VarDecl"))), TypeVar("t4"), ScopeVar("s1")),
      TermVar("x4", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t5"), ScopeVar("s1")),
      TermVar("x5", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s1"))
    )),
    SortAppl("Method"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2")))),
      Par(ScopeVar("s1"), ScopeVar("s")),
      Dec(ScopeVar("s1"), NameVar("n2"))
    )
  )

  // Param : Type * ID -> ParamDecl
  private val ruleParam = Rule(
    TermAppl("Param", List(
      TermVar("x", SortAppl("Type"), TypeVar("t"), ScopeVar("s")),
      NameVar("n")
    )),
    SortAppl("ParamDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), NameVar("n")),
      TypeOf(NameVar("n"), TypeVar("t"))
    )
  )

  // Var : Type * ID -> VarDecl
  private val ruleVar = Rule(
    TermAppl("Var", List(
      TermVar("x", SortAppl("Type"), TypeVar("t"), ScopeVar("s")),
      NameVar("n")
    )),
    SortAppl("VarDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), NameVar("n")),
      TypeOf(NameVar("n"), TypeVar("t"))
    )
  )

  // Field : Type * ID -> FieldDecl
  private val ruleField = Rule(
    TermAppl("Field", List(
      TermVar("x", SortAppl("Type"), TypeVar("t"), ScopeVar("s")),
      NameVar("n")
    )),
    SortAppl("FieldDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), NameVar("n")),
      TypeOf(NameVar("n"), TypeVar("t"))
    )
  )

  // Print : Exp -> Statement
  private val rulePrint = Rule(
    TermAppl("Print", List(
      TermVar("x", SortAppl("Exp"), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // While : Exp * Statement -> Statement
  private val ruleWhile = Rule(
    TermAppl("While", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Statement"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("Bool"))
    )
  )

  // If : Exp * Statement * Statement -> Statement
  private val ruleIf = Rule(
    TermAppl("If", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Statement"), TypeVar("t2"), ScopeVar("s1")),
      TermVar("x3", SortAppl("Statement"), TypeVar("t3"), ScopeVar("s2"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      Par(ScopeVar("s1"), ScopeVar("s")),
      Par(ScopeVar("s2"), ScopeVar("s"))
    )
  )

  //  Block : List(Statement) -> Statement
  private val ruleBlock = Rule(
    TermAppl("Block", List(
      TermVar("x1", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // ArrayAssign : ID * Exp * Exp -> Statement
  private val ruleArrayAssign = Rule(
    TermAppl("ArrayAssign", List(
      NameVar("n"),
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s")),
      Res(NameVar("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeAppl("IntArrayType")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Assign : ID * Exp -> Statement
  private val ruleAssign = Rule(
    TermAppl("Assign", List(
      NameVar("n"),
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s")),
      Res(NameVar("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t1"))
    )
  )

  // NewObject : ID -> Exp
  private val ruleNewObject = Rule(
    TermAppl("NewObject", List(
      NameVar("n")
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s")),
      Res(NameVar("n"), NameVar("d"))
    )
  )

  // Subscript : Exp * IndexExp -> Exp
  private val ruleSubscript = Rule(
    TermAppl("Subscript", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("IndexExp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("IntType")),
      TypeEquals(TypeVar("t1"), TypeAppl("IntArrayType")),
      TypeEquals(TypeVar("t1"), TypeAppl("IntType"))
    )
  )

  // Call : Exp * ID * List(Exp) -> Exp (TODO: Type of arguments goes wrong..)
  private val ruleCall = Rule(
    TermAppl("Subscript", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      NameVar("n"),
      TermVar("x2", SortAppl("List", List(SortAppl("Exp"))), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("ClassType", List(NameVar("d1")))),
      Ref(NameVar("n"), ScopeVar("s2")),
      Res(NameVar("n"), NameVar("d2")),
      TypeOf(NameVar("d2"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t"))))
    )
  )

  // This : Exp


  // Length : Exp -> Exp
  private val ruleLength = Rule(
    TermAppl("Length", List(
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("IntArray")),
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  // NewArray : Exp -> Exp
  private val ruleNewArray = Rule(
    TermAppl("NewArray", List(
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t"), TypeAppl("IntArray"))
    )
  )

  // And : Exp * Exp -> Exp
  private val ruleAnd = Rule(
    TermAppl("And", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  //  Lt : Exp * Exp -> Exp
  private val ruleLt = Rule(
    TermAppl("Lt", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Mul : Exp * Exp -> Exp
  private val ruleMul = Rule(
    TermAppl("Lt", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Sub : Exp * Exp -> Exp
  private val ruleSub = Rule(
    TermAppl("Lt", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Add : Exp * Exp -> Exp
  private val ruleAdd = Rule(
    TermAppl("Add", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Not : Exp -> Exp
  private val ruleNot = Rule(
    TermAppl("Not", List(
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool"))
    )
  )

  // VarRef : ID -> VarRef
  private val ruleVarRef = Rule(
    TermAppl("VarRef", List(
      NameVar("n")
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s")),
      Res(NameVar("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t"))
    )
  )

  // ClassType : ID -> Type
  private val ruleClassType = Rule(
    TermAppl("ClassType", List(
      NameVar("n")
    )),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(NameVar("n"), ScopeVar("s")),
      Res(NameVar("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t"))
    )
  )

  private val ruleIntArrayType = Rule(
    TermAppl("IntArray"),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("IntArray"))
    )
  )

  private val ruleIntType = Rule(
    TermAppl("Int"),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleBoolType = Rule(
    TermAppl("Bool"),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  private val ruleIntValue = Rule(
    TermAppl("Number"),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Int"))
    )
  )

  private val ruleTrue = Rule(
    TermAppl("True"),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  private val ruleFalse = Rule(
    TermAppl("False"),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t"), TypeAppl("Bool"))
    )
  )

  private val ruleNil = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortVar("a"))),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  val ruleCons = Rule(
    TermAppl("Cons", List(
      TermVar("x", SortVar("a"), TypeVar("t"), ScopeVar("s")),
      TermVar("xs", SortAppl("List", List(SortVar("a"))), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("List", List(SortVar("a"))),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  val rules = List(
    ruleProgram,
    ruleMainClass,
    ruleClass,
    ruleParent,
    ruleNone,
    ruleMethod,
    ruleParam,
    ruleVar,
    ruleField,
    // Types
    ruleClassType,
    ruleIntArrayType,
    ruleIntType,
    ruleBoolType,
    // Statements
    rulePrint,
    ruleWhile,
    ruleIf,
    ruleBlock,
    ruleArrayAssign,
    ruleAssign,
    // Exp
    ruleNewObject,
    ruleSubscript,
    ruleCall,
    ruleLength,
    ruleNewArray,
    ruleAnd,
    ruleLt,
    ruleMul,
    ruleSub,
    ruleAdd,
    ruleNot,
    ruleVarRef,
    // Literals
    ruleIntValue,
    ruleTrue,
    ruleFalse,
    // Meta
    ruleNil,
    ruleCons
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
