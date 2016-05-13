package nl.tudelft.fragments.examples

import nl.tudelft.fragments._

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

  // MainClass : ID * ID * Statement -> MainClass (TODO: declaration for name of argument with type String(?))
  private val ruleMainClass = Rule(
    TermAppl("MainClass", List(
      PatternNameAdapter(SymbolicName("n1")),
      PatternNameAdapter(SymbolicName("n2")),
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
      PatternNameAdapter(SymbolicName("n")),
      TermVar("x1", SortAppl("ParentDecl"), TypeVar("t"), ScopeVar("s")),
      TermVar("x2", SortAppl("List", List(SortAppl("FieldDecl"))), TypeVar("t"), ScopeVar("s1")),
      TermVar("x3", SortAppl("List", List(SortAppl("MethodDecl"))), TypeVar("t"), ScopeVar("s1"))
    )),
    SortAppl("Class"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), SymbolicName("n")),
      TypeOf(SymbolicName("n"), TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("n"))))),
      Par(ScopeVar("s1"), ScopeVar("s")),
      AssocFact(NameVar("n"), ScopeVar("s1"))
    )
  )

  // Parent : ID -> ParentDecl
  private val ruleParent = Rule(
    TermAppl("Parent", List(
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("ParentDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d"))
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
      TermVar("x1", SortAppl("Type"), TypeVar("t1"), ScopeVar("s")),
      PatternNameAdapter(SymbolicName("n1")),
      TermVar("x2", SortAppl("List", List(SortAppl("ParamDecl"))), TypeVar("t3"), ScopeVar("s1")),
      TermVar("x3", SortAppl("List", List(SortAppl("VarDecl"))), TypeVar("t4"), ScopeVar("s1")),
      TermVar("x4", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t5"), ScopeVar("s1")),
      TermVar("x5", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s1"))
    )),
    SortAppl("MethodDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), SymbolicName("n1")),
      Par(ScopeVar("s1"), ScopeVar("s")),
      AssocFact(SymbolicName("n1"), ScopeVar("s1")),
      TypeOf(SymbolicName("n1"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t2"))))
    )
  )

  // Param : Type * ID -> ParamDecl
  private val ruleParam = Rule(
    TermAppl("Param", List(
      TermVar("x", SortAppl("Type"), TypeVar("t"), ScopeVar("s")),
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("ParamDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), SymbolicName("n")),
      TypeOf(SymbolicName("n"), TypeVar("t"))
    )
  )

  // Var : Type * ID -> VarDecl
  private val ruleVar = Rule(
    TermAppl("Var", List(
      TermVar("x", SortAppl("Type"), TypeVar("t"), ScopeVar("s")),
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("VarDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), SymbolicName("n")),
      TypeOf(SymbolicName("n"), TypeVar("t"))
    )
  )

  // Field : Type * ID -> FieldDecl
  private val ruleField = Rule(
    TermAppl("Field", List(
      TermVar("x", SortAppl("Type"), TypeVar("t1"), ScopeVar("s")),
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("FieldDecl"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Dec(ScopeVar("s"), SymbolicName("n")),
      TypeOf(SymbolicName("n"), TypeVar("t1"))
    )
  )

  // Print : Exp -> Statement
  private val rulePrint = Rule(
    TermAppl("Print", List(
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      TypeEquals(TypeVar("t1"), TypeAppl("Int"))
    )
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
      TermVar("x", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    Nil
  )

  // ArrayAssign : ID * Exp * Exp -> Statement
  private val ruleArrayAssign = Rule(
    TermAppl("ArrayAssign", List(
      PatternNameAdapter(SymbolicName("n")),
      TermVar("x1", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s")),
      TermVar("x2", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeAppl("IntArrayType")),
      TypeEquals(TypeVar("t1"), TypeAppl("Int")),
      TypeEquals(TypeVar("t2"), TypeAppl("Int"))
    )
  )

  // Assign : ID * Exp -> Statement
  private val ruleAssign = Rule(
    TermAppl("Assign", List(
      PatternNameAdapter(SymbolicName("n")),
      TermVar("x", SortAppl("Exp"), TypeVar("t1"), ScopeVar("s"))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t1"))
    )
  )

  // NewObject : ID -> Exp
  private val ruleNewObject = Rule(
    TermAppl("NewObject", List(
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
      TypeEquals(TypeVar("t"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d")))))
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

  // Call : Exp * ID * List(Exp) -> Exp (TODO: Type of arguments goes wrong, assoc is wrong)
  private val ruleCall = Rule(
    TermAppl("Call", List(
      TermVar("x1", SortAppl("Exp"), TypeVar("t2"), ScopeVar("s")),
      PatternNameAdapter(SymbolicName("n"))
      //TermVar("x2", SortAppl("List", List(SortAppl("Exp"))), TypeVar("t2"), ScopeVar("s"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      DirectImport(ScopeVar("s2"), ScopeVar("s3")),
      Ref(SymbolicName("n"), ScopeVar("s2")),
      AssocConstraint(NameVar("d1"), ScopeVar("s3")),
      Res(SymbolicName("n"), NameVar("d2")),
      TypeOf(NameVar("d2"), TypeAppl("Fun", List(TypeVar("t1"), TypeVar("t")))),
      TypeEquals(TypeVar("t2"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d1")))))
    )
  )

  // This : Exp (TODO: This requires concrete names in the generation)
  private val ruleThis = Rule(
    TermAppl("This"),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(

    )
  )

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
      TypeEquals(TypeVar("t"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t1"), TypeAppl("Bool")),
      TypeEquals(TypeVar("t2"), TypeAppl("Bool"))
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
    TermAppl("Mul", List(
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
    TermAppl("Sub", List(
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
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
      TypeOf(NameVar("d"), TypeVar("t"))
    )
  )

  // ClassType : ID -> Type
  private val ruleClassType = Rule(
    TermAppl("ClassType", List(
      PatternNameAdapter(SymbolicName("n"))
    )),
    SortAppl("Type"),
    TypeVar("t"),
    ScopeVar("s"),
    List(
      Ref(SymbolicName("n"), ScopeVar("s")),
      Res(SymbolicName("n"), NameVar("d")),
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
    TermAppl("IntValue"),
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
//    ruleThis,
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
    // Generic list
    ruleNil,
    ruleCons
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
