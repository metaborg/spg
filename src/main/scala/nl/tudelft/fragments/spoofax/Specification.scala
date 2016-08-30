package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.spoofax.SpoofaxScala._
import nl.tudelft.fragments.spoofax.models._
import nl.tudelft.fragments._
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoAppl, StrategoList}

// Representation of an NaBL2 specification
class Specification(val params: ResolutionParams, val rules: List[Rule])

// Representation of NaBL2 resolution parameters
class ResolutionParams(val labels: List[Label], val order: PartialOrdering[Label], val wf: Regex)

// Companion object
object Specification {
  // Start at 9 so we do not clash with names in the rules
  val nameProvider = NameProvider(9)

  // Parse an NaBL2 specification file
  def read(nablPath: String, specPath: String)(implicit signatures: List[Signature]): Specification = {
    val nablImpl = Utils.loadLanguage(nablPath)
    val ast = Utils.parseFile(nablImpl, specPath)

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
  def inlineRecurse(rule: Rule)(implicit signatures: List[Signature]) = {
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
  def getSort(p1: Pattern, p2: Pattern, sort: Option[Sort] = None)(implicit signatures: List[Signature]): Option[Sort] = (p1, p2) match {
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

      (children, sorts).zipped.foldLeft(Option.empty[Sort]) {
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
  def getSignature(pattern: Pattern)(implicit signatures: List[Signature]): Option[OpDecl] = pattern match {
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
        case _ =>
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
    * Convert the Rules(_) block to a list of Rule
    */
  def toRules(term: IStrategoTerm)(implicit signatures: List[Signature]): List[Rule] = {
    val rules = term.collectAll {
      case appl: IStrategoAppl if appl.getConstructor.getName == "CGenMatchRule" =>
        true
      case _ =>
        false
    }

    rules.zipWithIndex.map {
      case (appl: IStrategoAppl, index) if appl.getConstructor.getName == "CGenMatchRule" =>
        toRule(index, appl)
    }
  }

  /**
    * Turn a CGenRule into a Rule
    */
  def toRule(ruleIndex: Int, term: IStrategoTerm)(implicit signatures: List[Signature]): Rule = term match {
    case appl: StrategoAppl =>
      val pattern = toPattern(appl.getSubterm(1))
      val scopes = toScopeAppls(appl.getSubterm(2))

      val newScopesTerms: List[IStrategoTerm] = appl.getSubterm(4).collectAll {
        case appl: IStrategoAppl if appl.getConstructor.getName == "NewScopes" =>
          true
        case _ =>
          false
      }

      val newScopeNames = newScopesTerms.flatMap(term =>
        term.getSubterm(0).getAllSubterms.toList.map(toNewScope)
      )

      // A scope is concrete if either:
      //   a) it is marked as 'new ...'
      //   b) it is a scope parameters
      implicit val concrete = newScopeNames ++ scopes.map(_.name)

      Rule(
        sort = toSort(pattern),
        typ = toTypeOption(ruleIndex, appl.getSubterm(3)),
        scopes = scopes,
        state = State(
          pattern = pattern,
          constraints = toConstraints(ruleIndex, appl.getSubterm(4))
        )
      )
  }

  // Retrieve the sort for the given pattern from the signatures
  def toSort(pattern: Pattern)(implicit signatures: List[Signature]): Sort = {
    val sort = getSignature(pattern)

    toSort(sort.get.typ)
  }

  def toSort(typ: Type): Sort = typ match {
    case FunType(_, ConstType(sort)) =>
      sort
    case ConstType(sort) =>
      sort
  }

  // Turn a CGenMatch into a Pattern
  def toPattern(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), toPatternsList(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      TermAppl("Nil")
    case appl: StrategoAppl if appl.getConstructor.getName == "ListTail" =>
      val heads = toPatternsList(appl.getSubterm(0))
      val tail = toPattern(appl.getSubterm(1))

      heads.foldLeft(tail) { case (acc, x) =>
        TermAppl("Cons", List(x, acc))
      }
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

  def toTypeOption(ruleIndex: Int, term: IStrategoTerm): Option[Pattern] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NoType" =>
      None
    case appl: StrategoAppl if appl.getConstructor.getName == "Type" =>
      Some(toType(ruleIndex, appl.getSubterm(0)))
  }

  // Turn a Stratego type into a Type (represented as Pattern)
  def toType(ruleIndex: Int, term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      TermAppl("List", appl.getSubterm(0).getAllSubterms.toList.map(toType(ruleIndex, _)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), appl.getSubterm(1).getAllSubterms.map(toType(ruleIndex, _)).toList)
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      toName(ruleIndex, appl)
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
  def toName(ruleIndex: Int, term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      TermVar(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      if (appl.getSubterm(1).asInstanceOf[StrategoAppl].getConstructor.getName == "Str") {
        ConcreteName(toNamespace(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0)), ruleIndex)
      } else {
        SymbolicName(toNamespace(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0)))
      }
  }

  // Turn a Stratego namespace into a string. Use "Default" as a default namespace.
  def toNamespace(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Default" =>
      "Default"
    case appl =>
      toString(appl.getSubterm(0))
  }

  // Turn a Stratego list of constraints into a List[Constraint]
  def toConstraints(ruleIndex: Int, constraint: IStrategoTerm)(implicit vars: List[String]): List[Constraint] = constraint match {
    case list: StrategoList =>
      val constraints = list.getAllSubterms.toList.filter {
        case appl: StrategoAppl if appl.getConstructor.getName == "NewScopes" =>
          false
        case _ =>
          true
      }

      constraints.flatMap(toConstraint(ruleIndex, _))
  }

  // Turn a Stratego Var into a String
  def toNewScope(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      toString(appl.getSubterm(0))
  }

  // Turn a constraint into a Constarint
  def toConstraint(ruleIndex: Int, constraint: IStrategoTerm)(implicit vars: List[String]): Option[Constraint] = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CTrue" =>
      Some(CTrue())
    case appl: StrategoAppl if appl.getConstructor.getName == "CGRef" =>
      Some(CGRef(toName(ruleIndex, appl.getSubterm(0)), toScope(appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDecl" =>
      Some(CGDecl(toScope(appl.getSubterm(1)), toName(ruleIndex, appl.getSubterm(0))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CResolve" =>
      Some(CResolve(toName(ruleIndex, appl.getSubterm(0)), toName(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CTypeOf" =>
      Some(CTypeOf(toName(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDirectEdge" =>
      Some(CGDirectEdge(toScope(appl.getSubterm(0)), toLabel(appl.getSubterm(1)), toScope(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CEqual" =>
      Some(CEqual(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGAssoc" =>
      Some(CGAssoc(toName(ruleIndex, appl.getSubterm(0)), toScope(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CAssoc" =>
      Some(CAssoc(toName(ruleIndex, appl.getSubterm(0)), toScope(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CSubtype" =>
      Some(CSubtype(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "FSubtype" =>
      Some(FSubtype(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGNamedEdge" =>
      Some(CGNamedEdge(toScope(appl.getSubterm(2)), toLabel(appl.getSubterm(1)), toName(ruleIndex, appl.getSubterm(0))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRecurse" =>
      Some(CGenRecurse(toPattern(appl.getSubterm(1)), toScopeAppls(appl.getSubterm(2)), toTypeOption(ruleIndex, appl.getSubterm(3)), null))
    case appl: StrategoAppl if appl.getConstructor.getName == "CInequal" =>
      Some(CInequal(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CFalse" =>
      Some(CFalse())

    // Unsupported constraints, ignored by this method
    case appl: StrategoAppl if appl.getConstructor.getName == "CDistinct" =>
      None
    case appl: StrategoAppl if appl.getConstructor.getName == "CInequal" =>
      None
  }

  // Turn IStrategoString into String
  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }
}
