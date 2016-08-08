package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures
import nl.tudelft.fragments.spoofax.Signatures.{ConstType, FunType, OpDecl}

import scala.collection.mutable

object Completion {
  implicit val rules = List(
    Rule(SortAppl("List", List(SortVar("a"))), None, List(ScopeAppl("s")), State(TermAppl("Cons", List(Var("x"), Var("xs"))),List(CGenRecurse(Var("xs"),List(ScopeAppl("s")),None,SortAppl("List", List(SortVar("a")))), CGenRecurse(Var("x"),List(ScopeAppl("s")),None,SortVar("a"))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("List", List(SortVar("a"))), None, List(ScopeAppl("s")), State(TermAppl("Nil", List()),List(CTrue()),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Start", List()), None, List(ScopeAppl("s")), State(TermAppl("Program", List(Var("dd"), Var("e"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s")),Some(Var("t")),SortAppl("Exp", List())), CGenRecurse(Var("dd"),List(ScopeAppl("s")),None,SortAppl("List", List(SortAppl("Declaration", List()))))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(TermAppl("TInt", List())), List(ScopeAppl("s11")), State(TermAppl("IntValue", List(Var("x10"))),List(CTrue()),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Lhs", List()), Some(Var("t")), List(ScopeAppl("s")), State(TermAppl("Var", List(Var("x"))),List(CResolve(SymbolicName("Var", "x"),NameVar("d")), CTypeOf(NameVar("d"),Var("t"))),List(CGRef(SymbolicName("Var", "x"),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(TermAppl("TInt", List())), List(ScopeAppl("s")), State(TermAppl("Add", List(Var("e1"), Var("e2"))),List(CGenRecurse(Var("e2"),List(ScopeAppl("s")),Some(TermAppl("TInt", List())),SortAppl("Exp", List())), CGenRecurse(Var("e1"),List(ScopeAppl("s")),Some(TermAppl("TInt", List())),SortAppl("Exp", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(TermAppl("TFun", List(Var("t1"), Var("t2")))), List(ScopeAppl("s")), State(TermAppl("Fun", List(Var("x"), Var("t"), Var("e"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s'")),Some(Var("t2")),SortAppl("Exp", List())), CGenRecurse(Var("t"),List(ScopeAppl("s")),Some(Var("t1")),SortAppl("Type", List())), CTypeOf(SymbolicName("Var", "x"),Var("t1"))),List(CGDecl(ScopeAppl("s'"),SymbolicName("Var", "x")), CGDirectEdge(ScopeAppl("s'"),Label('P'),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(Var("t2")), List(ScopeAppl("s")), State(TermAppl("App", List(Var("e1"), Var("e2"))),List(CGenRecurse(Var("e2"),List(ScopeAppl("s")),Some(Var("t1'")),SortAppl("ResetExp", List())), CGenRecurse(Var("e1"),List(ScopeAppl("s")),Some(TermAppl("TFun", List(Var("t1"), Var("t2")))),SortAppl("Exp", List())), CSubtype(Var("t1'"),Var("t1"))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(TermAppl("TClass", List(Var("d")))), List(ScopeAppl("s")), State(TermAppl("NewObject", List(Var("x"))),List(CResolve(SymbolicName("Class", "x"),NameVar("d"))),List(CGRef(SymbolicName("Class", "x"),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(Var("ty2")), List(ScopeAppl("s")), State(TermAppl("Seq", List(Var("e1"), Var("e2"))),List(CGenRecurse(Var("e2"),List(ScopeAppl("s")),Some(Var("ty2")),SortAppl("Exp", List())), CGenRecurse(Var("e1"),List(ScopeAppl("s")),Some(Var("ty1")),SortAppl("Exp", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Declaration", List()), None, List(ScopeAppl("s")), State(TermAppl("Class", List(Var("x"), TermAppl("None", List()), Var("ff"))),List(CGenRecurse(Var("ff"),List(ScopeAppl("s'")),None,SortAppl("List", List(SortAppl("Field", List())))), CTypeOf(SymbolicName("Class", "x"),TermAppl("TClassDef", List(SymbolicName("Class", "x"))))),List(CGDecl(ScopeAppl("s"),SymbolicName("Class", "x")), CGAssoc(SymbolicName("Class", "x"),ScopeAppl("s'")), CGDirectEdge(ScopeAppl("s'"),Label('P'),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Field", List()), None, List(ScopeAppl("s")), State(TermAppl("Field", List(Var("x"), Var("t"), Var("e"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s")),Some(Var("ty'")),SortAppl("Exp", List())), CGenRecurse(Var("t"),List(ScopeAppl("s")),Some(Var("ty")),SortAppl("Type", List())), CTypeOf(SymbolicName("Var", "x"),Var("ty")), CSubtype(Var("ty'"),Var("ty"))),List(CGDecl(ScopeAppl("s"),SymbolicName("Var", "x"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Field", List()), None, List(ScopeAppl("s")), State(TermAppl("FieldOverride", List(Var("x"), Var("e"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s")),Some(Var("ty'")),SortAppl("Exp", List())), CResolve(SymbolicName("Var", "x"),NameVar("d")), CTypeOf(NameVar("d"),Var("ty")), CSubtype(Var("ty'"),Var("ty"))),List(CGRef(SymbolicName("Var", "x"),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Exp", List()), Some(Var("t")), List(ScopeAppl("s")), State(TermAppl("Assign", List(Var("lhs"), Var("e"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s")),Some(Var("t'")),SortAppl("Exp", List())), CGenRecurse(Var("lhs"),List(ScopeAppl("s")),Some(Var("t")),SortAppl("Lhs", List())), CSubtype(Var("t'"),Var("t"))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Lhs", List()), Some(Var("ty")), List(ScopeAppl("s")), State(TermAppl("QVar", List(Var("e"), Var("x"))),List(CGenRecurse(Var("e"),List(ScopeAppl("s")),Some(TermAppl("TClass", List(Var("d_class")))),SortAppl("Exp", List())), CAssoc(NameVar("d_class"),ScopeVar("cs")), CResolve(SymbolicName("Var", "x"),NameVar("d")), CTypeOf(NameVar("d"),Var("ty"))),List(CGDirectEdge(ScopeAppl("s'"),Label('I'),ScopeVar("cs")), CGRef(SymbolicName("Var", "x"),ScopeAppl("s'"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Type", List()), Some(TermAppl("TInt", List())), List(ScopeAppl("s")), State(TermAppl("IntType", List()),List(CTrue()),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Type", List()), Some(TermAppl("TFun", List(Var("t1'"), Var("t2'")))), List(ScopeAppl("s")), State(TermAppl("FunType", List(Var("t1"), Var("t2"))),List(CGenRecurse(Var("t2"),List(ScopeAppl("s")),Some(Var("t2'")),SortAppl("Type", List())), CGenRecurse(Var("t1"),List(ScopeAppl("s")),Some(Var("t1'")),SortAppl("Type", List()))),List(),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Type", List()), Some(TermAppl("TClass", List(Var("d")))), List(ScopeAppl("s")), State(TermAppl("ClassType", List(Var("x"))),List(CResolve(SymbolicName("Class", "x"),NameVar("d"))),List(CGRef(SymbolicName("Class", "x"),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List())),
    Rule(SortAppl("Type", List()), Some(TermAppl("TClassDef", List(Var("d")))), List(ScopeAppl("s")), State(TermAppl("ClassDefType", List(Var("x"))),List(CResolve(SymbolicName("Class", "x"),NameVar("d"))),List(CGRef(SymbolicName("Class", "x"),ScopeAppl("s"))),TypeEnv(),Resolution(),SubtypeRelation(List()),List()))
  )

  implicit val signatures = Signatures.read(
    strategoPath = "zip:/Users/martijn/Projects/spoofax-releng/stratego/org.metaborg.meta.lang.stratego/target/org.metaborg.meta.lang.stratego-2.0.0-SNAPSHOT.spoofax-language!/",
    signaturePath = "/Users/martijn/Projects/scopes-frames/L3/src-gen/signatures/L3-sig.str"
  )

  def main(args: Array[String]): Unit = {
    val rule = Rule(
      sort = SortAppl("Start"),
      typ = None,
      scopes = List(ScopeAppl("s1")),
      state = State(
        pattern = TermAppl("Program", List(
          Var("x1"),
          TermAppl("QVar", List(
            Var("x2"),
            SymbolicName("Field", "n3")
          ))
        )),
        constraints = List(
          CGDirectEdge(ScopeAppl("s3"), Label('I'), ScopeVar("s4")),
          CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")),
          CResolve(SymbolicName("Var", "n3"), NameVar("d2")),
          CGenRecurse(Var("x1"), List(ScopeAppl("s1")), None, SortAppl("List", List(SortAppl("Declaration")))),
          CGenRecurse(Var("x2"), List(ScopeAppl("s1")), Some(TermAppl("TClass", List(NameVar("d_class")))), SortAppl("Exp"))
        )
      )
    )

    // Fix rule, which has a reference n3 in scope s3 that cannot be resolved to a declaration
    val fixes = fix(rule, CGRef(SymbolicName("Var", "n3"), ScopeAppl("s3")))

    println(fixes)
  }

  // TODO: Procedure currently does not terminate...
  // Fix the reference in rule
  def fix(rule: Rule, ref: CGRef): List[Rule] = {
    val q = new mutable.Queue[Rule]
    q.enqueue(rule)

    while (q.nonEmpty) {
      val r = q.dequeue()

      // Check if we have a solution
      val newState = Solver.solveAny(r.state).head // TODO: We arbitrarily picked the head..

      // Check if we can resolve the reference to a declaration in the merged rule
      val declarations = Graph(newState.facts).res(newState.resolution)(ref.n)

      // If a declaration was added, return solution. Otherwise, recurse?
      if (declarations.nonEmpty) {
        return List(r)
      }

      val recurses = r.constraints.filter(_.isInstanceOf[CGenRecurse]).asInstanceOf[List[CGenRecurse]]

      // Try every recurse constraint
      for (recurse <- recurses) {
        // Compute rules that comply with the required sort (TODO: take injections into account)
        val applicables = rules.filter(_.sort.unify(recurse.sort).isDefined)

        // Try every applicable rule
        for (applicable <- applicables) {
          // Perform the transformation (i.e. merge)
          val merged = r.merge(recurse, applicable)

          // Merge may not have succeeded
          merged.foreach(q.enqueue(_))
        }
      }
    }

    return Nil
  }

  // Compute the sort for the placeholder in the tree according to the signatures
  def sort(tree: Pattern, plhdr: Var): List[Sort] = tree match {
    case termAppl@TermAppl(cons, children) =>
      val sig = signature(termAppl.cons)

      children.zipWithIndex.flatMap { case (child, index) =>
        val s = sig.get.typ.asInstanceOf[FunType].children(index).asInstanceOf[ConstType].sort

        if (child == plhdr) {
          List(s)
        } else {
          sort(child, s, plhdr)
        }
      }
    case _ =>
      Nil
  }

  def sort(tree: Pattern, ss: Sort, plhdr: Var): List[Sort] = tree match {
    case termAppl@TermAppl(cons, children) =>
      val sig = signature(termAppl.cons)

      sig.get.typ match {
        case funType: FunType =>
          val resultSort = funType.result.asInstanceOf[ConstType].sort
          val sortBinding = resultSort.unify(ss)

          children.zipWithIndex.flatMap { case (child, index) =>
            val s = funType.children(index).asInstanceOf[ConstType].sort

            if (child == plhdr) {
              List(s.substituteSort(sortBinding.get))
            } else {
              sort(child, s, plhdr)
            }
          }
        case _ =>
          Nil
      }
    case _ =>
      Nil
  }

  // Get the signature for constructor
  def signature(cons: String): Option[OpDecl] =
    signatures
      .filter(_.isInstanceOf[OpDecl])
      .asInstanceOf[List[OpDecl]]
      .find(_.name == cons)
}
