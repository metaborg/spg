package nl.tudelft.fragments.examples

import nl.tudelft.fragments._

object MiniJava {
  // Minimal size needed to root from given sort
  val up = Map[Sort, Int](
    SortAppl("Program") -> 0,
    SortAppl("MainClass") -> 2,
    SortAppl("List", List(SortAppl("ClassDecl"))) -> 6,
    SortAppl("ClassDecl") -> 8,
    SortAppl("ParentDecl") -> 12,
    SortAppl("List", List(SortAppl("FieldDecl"))) -> 12,
    SortAppl("FieldDecl") -> 14,
    SortAppl("List", List(SortAppl("MethodDecl"))) -> 12,
    SortAppl("MethodDecl") -> 14,
    SortAppl("List", List(SortAppl("VarDecl"))) -> 20,
    SortAppl("VarDecl") -> 22,
    SortAppl("List", List(SortAppl("ParamDecl"))) -> 20,
    SortAppl("ParamDecl") -> 22,
    SortAppl("Type") -> 16,
    SortAppl("List", List(SortAppl("Statement"))) -> 20,
    SortAppl("Statement") -> 22,
    SortAppl("List", List(SortAppl("Exp", List()))) -> 20,
    SortAppl("Exp") -> 20
  )

  // Minimal size needed to bottom from given sort
  val down = Map[Sort, Int](
    SortAppl("Program") -> 7,
    SortAppl("MainClass") -> 5,
    SortAppl("List", List(SortAppl("ClassDecl"))) -> 1,
    SortAppl("ClassDecl") -> 5,
    SortAppl("ParentDecl") -> 1,
    SortAppl("List", List(SortAppl("FieldDecl"))) -> 1,
    SortAppl("FieldDecl") -> 3,
    SortAppl("List", List(SortAppl("MethodDecl"))) -> 1,
    SortAppl("MethodDecl") -> 7,
    SortAppl("List", List(SortAppl("VarDecl"))) -> 1,
    SortAppl("VarDecl") -> 3,
    SortAppl("List", List(SortAppl("ParamDecl"))) -> 1,
    SortAppl("ParamDecl") -> 3,
    SortAppl("Type") -> 1,
    SortAppl("List", List(SortAppl("Statement"))) -> 1,
    SortAppl("Statement") -> 2,
    SortAppl("List", List(SortAppl("Exp", List()))) -> 1,
    SortAppl("Exp") -> 1
  )

  // Sort needed to go to root quickest
  val root = Map[Sort, Sort](
    SortAppl("MainClass") -> SortAppl("Program"),
    SortAppl("List", List(SortAppl("ClassDecl"))) -> SortAppl("Program"),
    SortAppl("ClassDecl") -> SortAppl("List", List(SortAppl("ClassDecl"))),
    SortAppl("List", List(SortAppl("MethodDecl"))) -> SortAppl("ClassDecl"),
    SortAppl("MethodDecl") -> SortAppl("List", List(SortAppl("MethodDecl"))),
    SortAppl("Statement") -> SortAppl("MethodDecl"),
    SortAppl("List", List(SortAppl("Exp", List()))) -> SortAppl("Exp"),
    SortAppl("Exp") -> SortAppl("Statement")
  )

  // Program : MainClass * List(ClassDecl) -> Program
  private val ruleProgram = Rule(
    TermAppl("Program", List(
      Var("x1", SortAppl("MainClass"), TypeVar("t"), List(ScopeVar("s"))),
      Var("x2", SortAppl("List", List(SortAppl("ClassDecl"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("Program"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  // MainClass : ID * ID * Statement -> MainClass
  private val ruleMainClass = Rule(
    TermAppl("MainClass", List(
      PatternNameAdapter(SymbolicName("Class", "n1")),
      PatternNameAdapter(SymbolicName("Method", "n2")),
      Var("x", SortAppl("Statement"), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("MainClass"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  // Class : ID * ParentDecl * List(FieldDecl) * List(MethodDecl) -> ClassDecl
  private val ruleClass = Rule(
    TermAppl("Class", List(
      PatternNameAdapter(SymbolicName("Class", "n")),
      Var("x1", SortAppl("ParentDecl"), TypeVar("t1"), List(ScopeVar("s"), ScopeVar("s1"))),
      Var("x2", SortAppl("List", List(SortAppl("FieldDecl"))), TypeVar("t2"), List(ScopeVar("s1"))),
      Var("x3", SortAppl("List", List(SortAppl("MethodDecl"))), TypeVar("t3"), List(ScopeVar("s1")))
    )),
    SortAppl("ClassDecl"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Class", "n")),
      CTypeOf(SymbolicName("Class", "n"), TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n"))))),
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGDecl(ScopeVar("s1"), ConcreteName("Implicit", "this", 1)),
      CTypeOf(ConcreteName("Implicit", "this", 1), TypeAppl("ClassType", List(TypeNameAdapter(SymbolicName("Class", "n"))))),
      CGAssoc(SymbolicName("Class", "n"), ScopeVar("s1"))
    ))
  )

  // Parent : ID -> ParentDecl // TODO: Import scope (inheritance), supertyping fact
  private val ruleParent = Rule(
    TermAppl("Parent", List(
      PatternNameAdapter(SymbolicName("Class", "n"))
    )),
    SortAppl("ParentDecl"),
    TypeVar("t"),
    List(ScopeVar("s1"), ScopeVar("s2")),
    State(List(
      CGRef(SymbolicName("Class", "n"), ScopeVar("s1")),
      CResolve(SymbolicName("Class", "n"), NameVar("d")),
      CGNamedEdge(ScopeVar("s2"), SymbolicName("Class", "n"))
    ))
  )

  // None : ParentDecl
  private val ruleNone = Rule(
    TermAppl("None"),
    SortAppl("ParentDecl"),
    TypeVar("t"),
    List(ScopeVar("s1"), ScopeVar("s2")),
    State(Nil)
  )

  // Method : Type * ID * List(ParamDecl) * List(VarDecl) * List(Statement) * Exp -> MethodDecl
  private val ruleMethod = Rule(
    TermAppl("Method", List(
      Var("x1", SortAppl("Type"), TypeVar("t2"), List(ScopeVar("s"))),
      PatternNameAdapter(SymbolicName("Method", "n1")),
      Var("x2", SortAppl("List", List(SortAppl("ParamDecl"))), TypeVar("t1"), List(ScopeVar("s1"))),
      Var("x3", SortAppl("List", List(SortAppl("VarDecl"))), TypeVar("t4"), List(ScopeVar("s1"))),
      Var("x4", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t5"), List(ScopeVar("s1"))),
      Var("x5", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s1")))
    )),
    SortAppl("MethodDecl"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Method", "n1")),
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGAssoc(SymbolicName("Method", "n1"), ScopeVar("s1")),
      CTypeOf(SymbolicName("Method", "n1"), TypeAppl("Pair", List(TypeVar("t1"), TypeVar("t2"))))
    ))
  )

  // Param : Type * ID -> ParamDecl
  private val ruleParam = Rule(
    TermAppl("Param", List(
      Var("x", SortAppl("Type"), TypeVar("t1"), List(ScopeVar("s"))),
      PatternNameAdapter(SymbolicName("Variable", "n"))
    )),
    SortAppl("ParamDecl"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Variable", "n")),
      CTypeOf(SymbolicName("Variable", "n"), TypeVar("t1"))
    ))
  )

  // Var : Type * ID -> VarDecl
  private val ruleVar = Rule(
    TermAppl("Var", List(
      Var("x", SortAppl("Type"), TypeVar("t1"), List(ScopeVar("s"))),
      PatternNameAdapter(SymbolicName("Variable", "n"))
    )),
    SortAppl("VarDecl"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Variable", "n")),
      CTypeOf(SymbolicName("Variable", "n"), TypeVar("t1"))
    ))
  )

  // Field : Type * ID -> FieldDecl
  private val ruleField = Rule(
    TermAppl("Field", List(
      Var("x", SortAppl("Type"), TypeVar("t1"), List(ScopeVar("s"))),
      PatternNameAdapter(SymbolicName("Variable", "n"))
    )),
    SortAppl("FieldDecl"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDecl(ScopeVar("s"), SymbolicName("Variable", "n")),
      CTypeOf(SymbolicName("Variable", "n"), TypeVar("t1"))
    ))
  )

  // Print : Exp -> Statement
  private val rulePrint = Rule(
    TermAppl("Print", List(
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("Int"))
    ))
  )

  // While : Exp * Statement -> Statement
  private val ruleWhile = Rule(
    TermAppl("While", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Statement"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("Bool"))
    ))
  )

  // If : Exp * Statement * Statement -> Statement
  private val ruleIf = Rule(
    TermAppl("If", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Statement"), TypeVar("t2"), List(ScopeVar("s1"))),
      Var("x3", SortAppl("Statement"), TypeVar("t3"), List(ScopeVar("s2")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("Bool")),
      CGDirectEdge(ScopeVar("s1"), ScopeVar("s")),
      CGDirectEdge(ScopeVar("s2"), ScopeVar("s"))
    ))
  )

  //  Block : List(Statement) -> Statement
  private val ruleBlock = Rule(
    TermAppl("Block", List(
      Var("x", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  // ArrayAssign : ID * Exp * Exp -> Statement
  private val ruleArrayAssign = Rule(
    TermAppl("ArrayAssign", List(
      PatternNameAdapter(SymbolicName("Variable", "n")),
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Variable", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Variable", "n"), NameVar("d")),
      CTypeOf(NameVar("d"), TypeAppl("IntArray")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  // Assign : ID * Exp -> Statement
  private val ruleAssign = Rule(
    TermAppl("Assign", List(
      PatternNameAdapter(SymbolicName("Variable", "n")),
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Statement"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Variable", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Variable", "n"), NameVar("d")),
      CTypeOf(NameVar("d"), TypeVar("t1"))
    ))
  )

  // NewObject : ID -> Exp
  private val ruleNewObject = Rule(
    TermAppl("NewObject", List(
      PatternNameAdapter(SymbolicName("Class", "n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Class", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Class", "n"), NameVar("d")),
      CEqual(TypeVar("t"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d")))))
    ))
  )

  // Subscript : Exp * IndexExp -> Exp
  private val ruleSubscript = Rule(
    TermAppl("Subscript", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("IndexExp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int")),
      CEqual(TypeVar("t1"), TypeAppl("IntArray")),
      CEqual(TypeVar("t1"), TypeAppl("Int"))
    ))
  )

  // Call : Exp * ID * List(Exp) -> Exp (TODO: Type of arguments goes wrong, assoc is wrong)
  private val ruleCall = Rule(
    TermAppl("Call", List(
      Var("x1", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s"))),
      PatternNameAdapter(SymbolicName("Method", "n")),
      Var("x2", SortAppl("List", List(SortAppl("Exp"))), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGDirectEdge(ScopeVar("s2"), Label('I'), ScopeVar("s3")),
      CGRef(SymbolicName("Method", "n"), ScopeVar("s2")),
      CAssoc(NameVar("d1"), ScopeVar("s3")),
      CResolve(SymbolicName("Method", "n"), NameVar("d2")),
      CTypeOf(NameVar("d2"), TypeAppl("Pair", List(TypeVar("t1"), TypeVar("t")))),
      CEqual(TypeVar("t2"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d1")))))
    ))
  )

  // This : Exp
  private val ruleThis = Rule(
    TermAppl("This"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(ConcreteName("Implicit", "this", 1), ScopeVar("s")),
      CResolve(ConcreteName("Implicit", "this", 1), NameVar("d")),
      CTypeOf(NameVar("d"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d2"))))),
      CEqual(TypeVar("t"), TypeAppl("ClassType", List(TypeNameAdapter(NameVar("d2")))))
    ))
  )

  // Length : Exp -> Exp
  private val ruleLength = Rule(
    TermAppl("Length", List(
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("IntArray")),
      CEqual(TypeVar("t"), TypeAppl("Int"))
    ))
  )

  // NewArray : Exp -> Exp
  private val ruleNewArray = Rule(
    TermAppl("NewArray", List(
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t"), TypeAppl("IntArray"))
    ))
  )

  // And : Exp * Exp -> Exp
  private val ruleAnd = Rule(
    TermAppl("And", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool")),
      CEqual(TypeVar("t1"), TypeAppl("Bool")),
      CEqual(TypeVar("t2"), TypeAppl("Bool"))
    ))
  )

  //  Lt : Exp * Exp -> Exp
  private val ruleLt = Rule(
    TermAppl("Lt", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  // Mul : Exp * Exp -> Exp
  private val ruleMul = Rule(
    TermAppl("Mul", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  // Sub : Exp * Exp -> Exp
  private val ruleSub = Rule(
    TermAppl("Sub", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  // Add : Exp * Exp -> Exp
  private val ruleAdd = Rule(
    TermAppl("Add", List(
      Var("x1", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("x2", SortAppl("Exp"), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int")),
      CEqual(TypeVar("t1"), TypeAppl("Int")),
      CEqual(TypeVar("t2"), TypeAppl("Int"))
    ))
  )

  // Not : Exp -> Exp
  private val ruleNot = Rule(
    TermAppl("Not", List(
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s")))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool")),
      CEqual(TypeVar("t1"), TypeAppl("Bool"))
    ))
  )

  // VarRef : ID -> VarRef
  private val ruleVarRef = Rule(
    TermAppl("VarRef", List(
      PatternNameAdapter(SymbolicName("Variable", "n"))
    )),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Variable", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Variable", "n"), NameVar("d")),
      CTypeOf(NameVar("d"), TypeVar("t"))
    ))
  )

  // ClassType : ID -> Type
  private val ruleClassType = Rule(
    TermAppl("ClassType", List(
      PatternNameAdapter(SymbolicName("Class", "n"))
    )),
    SortAppl("Type"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CGRef(SymbolicName("Class", "n"), ScopeVar("s")),
      CResolve(SymbolicName("Class", "n"), NameVar("d")),
      CTypeOf(NameVar("d"), TypeVar("t"))
    ))
  )

  private val ruleIntArrayType = Rule(
    TermAppl("IntArray"),
    SortAppl("Type"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("IntArray"))
    ))
  )

  private val ruleIntType = Rule(
    TermAppl("Int"),
    SortAppl("Type"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int"))
    ))
  )

  private val ruleBoolType = Rule(
    TermAppl("Bool"),
    SortAppl("Type"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool"))
    ))
  )

  private val ruleIntValue = Rule(
    TermAppl("IntValue"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Int"))
    ))
  )

  private val ruleTrue = Rule(
    TermAppl("True"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool"))
    ))
  )

  private val ruleFalse = Rule(
    TermAppl("False"),
    SortAppl("Exp"),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Bool"))
    ))
  )

  private val ruleNilClassDecl = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("ClassDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleConsClassDecl = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("ClassDecl"), TypeVar("t"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("ClassDecl"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("ClassDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleNilFieldDecl = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("FieldDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleConsFieldDecl = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("FieldDecl"), TypeVar("t"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("FieldDecl"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("FieldDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleNilMethodDecl = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("MethodDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleConsMethodDecl = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("MethodDecl"), TypeVar("t"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("MethodDecl"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("MethodDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleNilParamDecl = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("ParamDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Nil"))
    ))
  )

  private val ruleConsParamDecl = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("ParamDecl"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("ParamDecl"))), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("ParamDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Cons", List(TypeVar("t1"), TypeVar("t2"))))
    ))
  )

  private val ruleNilVarDecl = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("VarDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleConsVarDecl = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("VarDecl"), TypeVar("t"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("VarDecl"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("VarDecl"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleNilStatement = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("Statement"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleConsStatement = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("Statement"), TypeVar("t"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("Statement"))), TypeVar("t"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("Statement"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(Nil)
  )

  private val ruleNilExp = Rule(
    TermAppl("Nil"),
    SortAppl("List", List(SortAppl("Exp"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Nil"))
    ))
  )

  private val ruleConsExp = Rule(
    TermAppl("Cons", List(
      Var("x", SortAppl("Exp"), TypeVar("t1"), List(ScopeVar("s"))),
      Var("xs", SortAppl("List", List(SortAppl("Exp"))), TypeVar("t2"), List(ScopeVar("s")))
    )),
    SortAppl("List", List(SortAppl("Exp"))),
    TypeVar("t"),
    List(ScopeVar("s")),
    State(List(
      CEqual(TypeVar("t"), TypeAppl("Cons", List(TypeVar("t1"), TypeVar("t2"))))
    ))
  )

  val rules = List(
    ruleProgram,
    ruleMainClass,
    ruleClass,
    ruleParent,
    ruleNone,
    ruleMethod,
    ruleParam,
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
    //Temporary disable calls, as they make generation complex (performance)
    //ruleCall,
    ruleThis,
    ruleVar,
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
    ruleNilClassDecl,
    ruleConsClassDecl,
    ruleNilFieldDecl,
    ruleConsFieldDecl,
    ruleNilMethodDecl,
    ruleConsMethodDecl,
    ruleNilParamDecl,
    ruleConsParamDecl,
    ruleNilVarDecl,
    ruleConsVarDecl,
    ruleNilStatement,
    ruleConsStatement,
    ruleNilExp,
    ruleConsExp
  )

  val types = List(
    TypeAppl("Int"),
    TypeAppl("Bool")
  )
}
