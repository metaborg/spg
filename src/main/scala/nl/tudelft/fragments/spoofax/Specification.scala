package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments
import nl.tudelft.fragments.spoofax.Signatures._
import nl.tudelft.fragments.spoofax.SpoofaxScala._
import nl.tudelft.fragments.{CAssoc, CEqual, CGAssoc, CGDecl, CGDirectEdge, CGNamedEdge, CGRef, CGenRecurse, CResolve, CSubtype, CTrue, CTypeOf, Character, Concatenation, Constraint, EmptySet, Epsilon, FSubtype, Intersection, Label, LabelOrdering, Main, NameProvider, Pattern, Regex, Resolution, Rule, Scope, ScopeAppl, ScopeVar, Star, State, SymbolicName, TermAppl, TermVar, Union}
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoAppl, StrategoList}

// Representation of an NaBL2 specification
class Specification(val params: ResolutionParams, val rules: List[Rule])

// Representation of NaBL2 resolution parameters
class ResolutionParams(val labels: List[Label], val order: PartialOrdering[Label], val wf: Regex)

// Companion object
object Specification {
  // TODO: Can we make Main.spoofax globally available or something? Or use DI, since it is just a dependency?
  val s = Main.spoofax

  // Start at 9 so we do not clash with names in the rules
  val nameProvider = NameProvider(9)

  // Parse an NaBL2 specification file
  def read(nablPath: String, specPath: String)(implicit signatures: List[Decl]): Specification = {
    val nablImpl = Utils.loadLanguage(nablPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(specPath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)
    val ast = parseResult.ast()

    // Translate ATerms to Scala DSL
    val labels = toLabels(ast)
    val ordering = toOrdering(ast)
    val wf = toWF(ast)
    val params = new ResolutionParams(labels, ordering, wf)
    val rules = toRules(ast.getSubterm(1)).map(inlineRecurse)
    val specification = new Specification(params, rules)

    specification
  }

  /**
    * Add sort to the Recurse constraints based on the position
    */
  def inlineRecurse(rule: Rule)(implicit signatures: List[Decl]) = {
    rule.recurse.foldLeft(rule) { case (rule, r@CGenRecurse(variable, scopes, typ, null)) =>
      rule.copy(
        state = rule.state.copy(
          constraints = CGenRecurse(variable, scopes, typ, getSort(rule.pattern, variable).get) :: rule.state.constraints - r
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
    * Convert the labels and augment with default labels
    */
  def toLabels(term: IStrategoTerm): List[Label] = {
    val customLabels = term
      .collectAll {
        case appl: StrategoAppl if appl.getConstructor.getName == "Label" =>
          true
        case x =>
          false
      }
      .distinct
      .map {
        case appl: StrategoAppl =>
          Label(toString(appl.getSubterm(0)).head)
      }

      Label('D') :: Label('I') :: Label('P') :: customLabels
  }

  // Turn a Stratego term into Label. TODO: Labels should be strings instead of chars, and Regex should use Labels as primitive unit
  def toLabel(term: IStrategoTerm): Label = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Label" =>
      Label(toString(appl.getSubterm(0)).head)
    case appl: StrategoAppl if appl.getConstructor.getName == "D" =>
      Label('D')
    case appl: StrategoAppl if appl.getConstructor.getName == "P" =>
      Label('P')
    case appl: StrategoAppl if appl.getConstructor.getName == "I" =>
      Label('I')
  }

  /**
    * Convert the label ordering and augment with default ordering
    */
  def toOrdering(term: IStrategoTerm): LabelOrdering = {
    val pairs = term
      .collectAll {
        case appl: StrategoAppl if appl.getConstructor.getName == "Lt" =>
          true
        case x =>
          false
      }
      .map(term =>
        toLabel(term.getSubterm(0)) -> toLabel(term.getSubterm(1))
      )

    val defaultOrdering = List(
      Label('D') -> Label('P'),
      Label('D') -> Label('I'),
      Label('I') -> Label('P')
    )

    LabelOrdering(pairs ++ defaultOrdering: _*)
  }

  /**
    * Convert the WF or fall back to default WF
    */
  def toWF(term: IStrategoTerm): Regex = {
    val customWf = term
      .collect {
        case appl: StrategoAppl if appl.getConstructor.getName == "WF" =>
          true
        case x =>
          false
      }
      .map(term =>
        toRegex(term.getSubterm(0))
      )

    customWf match {
      case None =>
        (Character('P') *) ~ (Character('I') *)
      case Some(wf) =>
        wf
    }
  }

  /**
    * Convert the regex to our Scala DSL
    */
  def toRegex(term: IStrategoTerm): Regex = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Concat" =>
      Concatenation(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Or" =>
      Union(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "And" =>
      Intersection(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Closure" =>
      Star(toRegex(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Epsilon" =>
      Epsilon
    case appl: StrategoAppl if appl.getConstructor.getName == "Empty" =>
      EmptySet
    case appl: StrategoAppl if appl.getConstructor.getName == "Label" =>
      Character(toString(appl.getSubterm(0)).head)
    case appl: StrategoAppl if appl.getConstructor.getName == "P" =>
      Character('P')
    case appl: StrategoAppl if appl.getConstructor.getName == "I" =>
      Character('I')
  }

  /**
    * Convert the constraint generation rules (CGenRules) to our Scala DSL
    */
  def toRules(term: IStrategoTerm)(implicit signatures: List[Decl]): List[Rule] = term match {
    case list: StrategoList =>
      list.getAllSubterms.toList.flatMap {
        case appl: IStrategoAppl if appl.getConstructor.getName == "CGenRules" =>
          toRulesList(appl.getSubterm(0))
        case _ =>
          Nil
      }
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
      val scopes = toScopeAppls(appl.getSubterm(0).getSubterm(2))

      // A scope is concrete if either:
      //   a) it is marked as 'new ...'
      //   b) it is a scope parameters
      implicit val concrete = toNewList(appl.getSubterm(2)) ++ scopes.map(_.name)

      Rule(
        sort = toSort(pattern),
        typ = toTypeOption(appl.getSubterm(0).getSubterm(3)),
        scopes = scopes,
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

  def toTypeOption(term: IStrategoTerm): Option[Pattern] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NoType" =>
      None
    case typ =>
      Some(toType(typ))
  }

  // Turn a Stratego type into a Type (represented as Pattern)
  def toType(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), appl.getSubterm(1).getAllSubterms.map(toType).toList)
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      toName(appl)
  }

  // Turn a Stratego scope into a ScopeAppl
  def toScopeAppl(term: IStrategoTerm): ScopeAppl = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      ScopeAppl(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      ScopeAppl("s" + nameProvider.next)
  }

  // Turn a Stratego scope into a Scope
  def toScope(term: IStrategoTerm)(implicit concrete: List[String]): Scope = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      if (concrete.contains(toString(appl.getSubterm(0)))) {
        ScopeAppl(toString(appl.getSubterm(0)))
      } else {
        ScopeVar(toString(appl.getSubterm(0)))
      }
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      ScopeVar("s" + nameProvider.next)
  }

  // Turn a list of Stratego scopes into a List[Scope]
  def toScopes(term: IStrategoTerm)(implicit concrete: List[String]): List[Scope] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      toScopes(appl.getSubterm(0))
    case appl: IStrategoList if appl.isEmpty =>
      Nil
    case appl: IStrategoList =>
      toScope(appl.head()) :: toScopes(appl.tail())
  }

  // Turn a list of Stratego scopes into a List[ScopeAppl]
  def toScopeAppls(term: IStrategoTerm): List[ScopeAppl] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      toScopeAppls(appl.getSubterm(0))
    case appl: IStrategoList if appl.isEmpty =>
      Nil
    case appl: IStrategoList =>
      toScopeAppl(appl.head()) :: toScopeAppls(appl.tail())
  }

  // Turn a Stratego name into a Name
  def toName(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(toString(appl.getSubterm(0)))
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
  def toConstraints(constraint: IStrategoTerm)(implicit vars: List[String]): List[Constraint] = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CConj" =>
      toConstraint(appl.getSubterm(0)) :: toConstraints(appl.getSubterm(1))
    case appl: StrategoAppl =>
      List(toConstraint(appl))
  }

  // Turn a Stratego term into list of ScopeVars
  def toNewList(term: IStrategoTerm): List[String] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NoNew" =>
      Nil
    case appl: StrategoAppl if appl.getConstructor.getName == "New" =>
      toNewList(appl.getSubterm(0))
    case appl: StrategoList if appl.isEmpty =>
      Nil
    case list: IStrategoList =>
      toNew(list.head()) :: toNewList(list.tail())
  }

  // Turn a Stratego Var into a String
  def toNew(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      toString(appl.getSubterm(0))
  }

  // Turn a constraint into a Constarint
  def toConstraint(constraint: IStrategoTerm)(implicit vars: List[String]): Constraint = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CTrue" =>
      CTrue()
    case appl: StrategoAppl if appl.getConstructor.getName == "CGRef" =>
      CGRef(toName(appl.getSubterm(0)), toScope(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDecl" =>
      CGDecl(toScope(appl.getSubterm(1)), toName(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CResolve" =>
      CResolve(toName(appl.getSubterm(0)), toName(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CTypeOf" =>
      CTypeOf(toName(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDirectEdge" =>
      CGDirectEdge(toScope(appl.getSubterm(0)), toLabel(appl.getSubterm(1)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CEqual" =>
      CEqual(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGAssoc" =>
      CGAssoc(toName(appl.getSubterm(0)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CAssoc" =>
      CAssoc(toName(appl.getSubterm(0)), toScope(appl.getSubterm(2)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CSubtype" =>
      CSubtype(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "FSubtype" =>
      FSubtype(toType(appl.getSubterm(0)), toType(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGNamedEdge" =>
      CGNamedEdge(toScope(appl.getSubterm(2)), toLabel(appl.getSubterm(1)), toName(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRecurse" =>
      CGenRecurse(toPattern(appl.getSubterm(1)), toScopeAppls(appl.getSubterm(2)), toTypeOption(appl.getSubterm(3)), null)
  }

  // Turn IStrategoString into String
  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }
}
