package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments
import nl.tudelft.fragments.spoofax.Signatures._
import nl.tudelft.fragments.{Constraint, Dec, MainBuilder, Name, NameVar, Par, Pattern, PatternNameAdapter, Recurse, Ref, Res, Rule, Scope, ScopeVar, SortAppl, State, SymbolicName, TermAppl, TermVar, True, Type, TypeAppl, TypeEquals, TypeOf, TypeVar}
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoAppl, StrategoList}

// TODO: Handle injections. Now we have 'ResetSort' second sort in App constructor, but no constructors for ResetSort since it's an injection..
object Specification {
  val s = MainBuilder.spoofax

  /**
    * Parse the specification (constraint generation function)
    */
  def read(nablPath: String, specPath: String)(implicit signatures: List[Decl]): List[Rule] = {
    val nablImpl = Utils.loadLanguage(nablPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(specPath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)

    // Translate ATerms to Scala DSL
    val rules = toRules(parseResult.ast().getSubterm(1).getSubterm(2))

    rules
      .map(inlineRecurse)
  }

  /**
    * Add sort to the Recurse constraints based on the position
    */
  def inlineRecurse(rule: Rule)(implicit signatures: List[Decl]) = {
    rule.recurse.foldLeft(rule) { case (rule, r@Recurse(variable, scopes, typ, null)) =>
      rule.copy(
        state = rule.state.copy(
          constraints = Recurse(variable, scopes, typ, getSort(rule.pattern, variable).get) :: rule.state.constraints - r
        )
      )
    }
  }

  /**
    * Get sort for pattern p2 in pattern p1
    */
  def getSort(p1: Pattern, p2: Pattern, sort: Option[fragments.Sort] = None)(implicit signatures: List[Decl]): Option[fragments.Sort] = (p1, p2) match {
    case (_, _) if p1 == p2 =>
      sort
    case (termAppl@TermAppl(_, children), _) =>
      val signature = getSignature(p1).get

      val sorts = signature.typ match {
        case FunType(children, _) =>
          children
        case ConstType(_) =>
          Nil
      }

      (children, sorts).zipped.foldLeft(Option.empty[fragments.Sort]) {
        case (Some(x), _) =>
          Some(x)
        case (_, (child, sort)) =>
          getSort(child, p2, Some(toSort(sort)))
      }
    case _ =>
      None
  }

//  /**
//    * Inline the Recurse constraints with the corresonding TermVar
//    */
//  def inlineRecurse(rule: Rule) = {
//    val recurseConstraints = rule.constraints
//      .filter(_.isInstanceOf[Recurse])
//      .map(_.asInstanceOf[Recurse])
//
//    val inlinedRule = recurseConstraints.foldLeft(rule) { case (rule, Recurse(p@TermVar(name, _), _, _, _)) =>
//      rule.copy(state = rule.state.copy(
//        pattern = rule.state.pattern.substituteTerm(
//          Map(p -> TermVar(name, null))
//        )
//      ))
//    }
//
//    inlinedRule.copy(state =
//      inlinedRule.state.copy(constraints =
//        inlinedRule.state.constraints diff recurseConstraints
//      )
//    )
//  }
//
//  /**
//    * Inline the sort for TermVars
//    */
//  def inlineSort(pattern: Pattern)(implicit signatures: List[Decl]): Pattern = pattern match {
//    case termAppl@TermAppl(cons, children) =>
//      val signature = getSignature(pattern).get
//      val childSorts = signature.typ match {
//        case FunType(children, _) =>
//          children
//        case ConstType(_) =>
//          Nil
//      }
//
//      TermAppl(cons, (children, childSorts).zipped.map {
//        case (TermVar(name, null), ConstType(sort)) =>
//          TermVar(name, toSort(sort))
//      })
//  }

  /**
    * Get signature for the given Pattern
    */
  def getSignature(pattern: Pattern)(implicit signatures: List[Decl]): Option[OpDecl] = pattern match {
    case termAppl: TermAppl =>
      signatures
        .filter(_.isInstanceOf[OpDecl])
        .map(_.asInstanceOf[OpDecl])
        .find(_.name == termAppl.cons)
    case _ =>
      None
  }

  /**
    * Convert the constraint generation rules (ATerm) to our Scala DSL
    */
  def toRules(term: IStrategoTerm)(implicit signatures: List[Decl]): List[Rule] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRules" =>
      toRulesList(appl.getSubterm(0))
  }

  /**
    * Turn a Stratego list of CGenRule into a List[Rule]
    */
  def toRulesList(list: IStrategoTerm)(implicit signatures: List[Decl]): List[Rule] = list match {
    case list: StrategoList =>
      list.getAllSubterms
        .filter {
          case appl: StrategoAppl =>
            appl.getSubterm(0).asInstanceOf[StrategoAppl].getConstructor.getName != "CGenInit"
        }
        .map(toRule)
        .toList
  }

  /**
    * Turn a CGenRule into a Rule
    */
  def toRule(rule: IStrategoTerm)(implicit signatures: List[Decl]): Rule = rule match {
    case appl: StrategoAppl =>
      val pattern = toPattern(appl.getSubterm(0).getSubterm(1))

      Rule(
        sort = toSort(pattern),
        typ = toType(appl.getSubterm(0).getSubterm(3)),
        scopes = toScopes(appl.getSubterm(0).getSubterm(2)),
        state = State(
          pattern = pattern,
          constraints = toConstraints(appl.getSubterm(1))
        )
      )
  }

  /**
    * Retrieve the sort for the given pattern from the signatures
    */
  def toSort(pattern: Pattern)(implicit signatures: List[Decl]): fragments.Sort = {
    val sort = getSignature(pattern)

    toSort(sort.get.typ)
  }

  def toSort(typ: Signatures.Type): fragments.Sort = typ match {
    case FunType(children, ConstType(SortNoArgs(name))) =>
      SortAppl(name)
    case ConstType(SortNoArgs(name)) =>
      SortAppl(name)
  }

  def toSort(sort: Sort): fragments.Sort = sort match {
    case SortNoArgs(name) =>
      SortAppl(name)
  }

  /**
    * Turn a CGenMatch into a Pattern
    */
  def toPattern(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue(), toPatternsList(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue())
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      TermVar(null) // TODO: Model wildcards explicitly or just invent random names?
  }

  def toPatternsList(term: IStrategoTerm): List[Pattern] = term match {
    case list: IStrategoList if list.isEmpty =>
      Nil
    case list: IStrategoList =>
      toPattern(list.head()) :: toPatternsList(list.tail())
  }

  /**
    * Turn a Stratego type into a Type
    */
  def toType(term: IStrategoTerm): Type = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TypeVar(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue())
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TypeAppl(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue(), appl.getSubterm(1).getAllSubterms.map(toType).toList)
  }

  /**
    * Turn a Stratego scope into a Scope
    */
  def toScope(term: IStrategoTerm): Scope = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      ScopeVar(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue())
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      // TODO: invent new name for wildcards?
      ScopeVar(null)
  }

  /**
    * Turn a list of Stratego scopes into a List[Scope]
    */
  def toScopes(term: IStrategoTerm): List[Scope] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      toScopes(appl.getSubterm(0))
    case appl: IStrategoList if appl.isEmpty =>
      Nil
    case appl: IStrategoList =>
      toScope(appl.head()) :: toScopes(appl.tail())
  }

  /**
    * Turn a Stratego name into a Name
    */
  def toName(term: IStrategoTerm): Name = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      NameVar(appl.getSubterm(0).asInstanceOf[IStrategoString].stringValue())
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      SymbolicName(appl.getSubterm(0).getSubterm(0).asInstanceOf[IStrategoString].stringValue(), appl.getSubterm(1).getSubterm(0).asInstanceOf[IStrategoString].stringValue())
  }

  /**
    * Turn a Stratego list of constraints into a List[Constraint]
    */
  def toConstraints(constraint: IStrategoTerm): List[Constraint] = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CConj" =>
      toConstraint(appl.getSubterm(0)) :: toConstraints(appl.getSubterm(1))
    case appl: StrategoAppl =>
      List(toConstraint(appl))
  }

  /**
    * Turn a constraint into a Constarint
    */
  def toConstraint(constraint: IStrategoTerm): Constraint = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CTrue" =>
      True()
    case appl: StrategoAppl if appl.getConstructor.getName == "CGRef" =>
      Ref(toName(appl.getSubterm(0)), toScope(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDecl" =>
      Dec(toScope(appl.getSubterm(1)), toName(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CResolve" =>
      Res(toName(appl.getSubterm(0)), toName(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CTypeOf" =>
      TypeOf(toName(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDirectEdge" =>
      Par(toScope(appl.getSubterm(0)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CEqual" =>
      TypeEquals(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRecurse" =>
      Recurse(toPattern(appl.getSubterm(1)), toScopes(appl.getSubterm(2)), toType(appl.getSubterm(3)), null)
  }
}
