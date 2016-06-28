package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments
import nl.tudelft.fragments.spoofax.Signatures._
import nl.tudelft.fragments.{AssocConstraint, AssocFact, AssociatedImport, Constraint, Dec, DirectEdge, Label, Main, Name, NameProvider, NameVar, Pattern, Recurse, Ref, Res, Rule, Scope, ScopeVar, State, Subtype, Supertype, SymbolicName, TermAppl, TermVar, True, Type, TypeAppl, TypeEquals, TypeNameAdapter, TypeOf, TypeVar}
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoAppl, StrategoList}

object Specification {
  val s = Main.spoofax

  // Start at 9 so we do not clash with names in the rules
  val nameProvider = NameProvider(9)

  // Parse the specification (constraint generation function)
  def read(nablPath: String, specPath: String)(implicit signatures: List[Decl]): List[Rule] = {
    val nablImpl = Utils.loadLanguage(nablPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(specPath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)

    // Translate ATerms to Scala DSL
    toRules(parseResult.ast().getSubterm(1).getSubterm(2))
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
        typ = toTypeOption(appl.getSubterm(0).getSubterm(3)),
        scopes = toScopes(appl.getSubterm(0).getSubterm(2)),
        state = State(
          pattern = pattern,
          constraints = toConstraints(appl.getSubterm(1))
        )
      )
  }

  // Retrieve the sort for the given pattern from the signatures
  def toSort(pattern: Pattern)(implicit signatures: List[Decl]): fragments.Sort = {
    val sort = getSignature(pattern)

    toSort(sort.get.typ)
  }

  def toSort(typ: Signatures.Type): fragments.Sort = typ match {
    case FunType(_, ConstType(sort)) =>
      sort
    case ConstType(sort) =>
      sort
  }

  // Turn a CGenMatch into a Pattern
  def toPattern(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), toPatternsList(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      TermVar("x" + nameProvider.next)
  }

  def toPatternsList(term: IStrategoTerm): List[Pattern] = term match {
    case list: IStrategoList if list.isEmpty =>
      Nil
    case list: IStrategoList =>
      toPattern(list.head()) :: toPatternsList(list.tail())
  }

  def toTypeOption(term: IStrategoTerm): Option[Type] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NoType" =>
      None
    case typ =>
      Some(toType(typ))
  }

  // Turn a Stratego type into a Type
  def toType(term: IStrategoTerm): Type = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TypeVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TypeAppl(toString(appl.getSubterm(0)), appl.getSubterm(1).getAllSubterms.map(toType).toList)
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      TypeNameAdapter(toName(appl))
  }

  // Turn a Stratego scope into a Scope
  def toScope(term: IStrategoTerm): Scope = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      ScopeVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      ScopeVar("s" + nameProvider.next)
  }

  // Turn a list of Stratego scopes into a List[Scope]
  def toScopes(term: IStrategoTerm): List[Scope] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      toScopes(appl.getSubterm(0))
    case appl: IStrategoList if appl.isEmpty =>
      Nil
    case appl: IStrategoList =>
      toScope(appl.head()) :: toScopes(appl.tail())
  }

  // Turn a Stratego name into a Name
  def toName(term: IStrategoTerm): Name = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      NameVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      SymbolicName(toNamespace(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0)))
  }

  // Turn a Stratego namespace into a string. Use "Default" as a default namespace.
  def toNamespace(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Default" =>
      "Default"
    case appl =>
      toString(appl.getSubterm(0))
  }

  // Turn a Stratego list of constraints into a List[Constraint]
  def toConstraints(constraint: IStrategoTerm): List[Constraint] = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CConj" =>
      toConstraint(appl.getSubterm(0)) :: toConstraints(appl.getSubterm(1))
    case appl: StrategoAppl =>
      List(toConstraint(appl))
  }

  // Turn a constraint into a Constarint
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
      DirectEdge(toScope(appl.getSubterm(0)), toLabel(appl.getSubterm(1)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CEqual" =>
      TypeEquals(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGAssoc" =>
      AssocFact(toName(appl.getSubterm(0)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CAssoc" =>
      AssocConstraint(toName(appl.getSubterm(0)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CSubtype" =>
      Subtype(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "FSubtype" =>
      Supertype(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGNamedEdge" =>
      // TODO: AssociatedImports have a label as well
      AssociatedImport(toScope(appl.getSubterm(2)), toName(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRecurse" =>
      Recurse(toPattern(appl.getSubterm(1)), toScopes(appl.getSubterm(2)), toTypeOption(appl.getSubterm(3)), null)
  }

  // Turn a Stratego term into Label
  def toLabel(term: IStrategoTerm): Label = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "P" =>
      Label('P')
    case appl: IStrategoAppl if appl.getConstructor.getName == "I" =>
      Label('I')
    case appl: IStrategoAppl if appl.getConstructor.getName == "Label" =>
      // TODO: Labels should be strings instead of chars, and regex's should cope with strings as unit element
      Label(toString(appl).head)
  }

  // Turn IStrategoString into String
  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }
}
