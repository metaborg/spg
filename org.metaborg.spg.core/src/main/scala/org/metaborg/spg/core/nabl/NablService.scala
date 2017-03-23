package org.metaborg.spg.core.nabl

import java.nio.charset.StandardCharsets

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.IProject
import org.metaborg.core.resource.IResourceService
import org.metaborg.spg.core.regex._
import org.metaborg.spg.core.resolution.LabelImplicits._
import org.metaborg.spg.core.resolution.{Label, LabelOrdering}
import org.metaborg.spg.core.sdf.{Sort, SortVar}
import org.metaborg.spg.core.solver._
import org.metaborg.spg.core.spoofax.SpoofaxScala._
import org.metaborg.spg.core.stratego.Signature
import org.metaborg.spg.core.terms._
import org.metaborg.spg.core.{NameProvider, Rule}
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService
import org.metaborg.util.resource.FileSelectorUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}
import org.spoofax.terms.{StrategoAppl, StrategoList}

/**
  * The NaBL service loads all constraint generation rules from a Spoofax
  * project or a single NaBL2 file.
  *
  * @param resourceService
  * @param unitService
  * @param syntaxService
  */
class NablService @Inject()(val resourceService: IResourceService, val unitService: ISpoofaxUnitService, val syntaxService: ISpoofaxSyntaxService) extends LazyLogging {
  // Start at 9 so we do not clash with names in the rules
  val nameProvider = NameProvider(9)

  /**
    * Read all NaBL2 files in the given project and collect the productions.
    *
    * @param nablLangImpl
    * @param project
    * @return
    */
  def read(nablLangImpl: ILanguageImpl, project: IProject)(implicit signatures: Signature): Specification = {
    val fileSelector = FileSelectorUtils.extension("nabl2")
    val files = project.location().findFiles(fileSelector).toList

    files
      .map(read(nablLangImpl, _))
      .foldLeft(Specification.empty)(_ merge _)
  }

  /**
    * Parse an NaBL2 file and extract a specification.
    *
    * @param nablLangImpl
    * @param specification
    * @param signatures
    * @return
    */
  def read(nablLangImpl: ILanguageImpl, specification: FileObject)(implicit signatures: Signature): Specification = {
    def parseFile(languageImpl: ILanguageImpl): IStrategoTerm = {
      val text = IOUtils.toString(specification.getContent.getInputStream, StandardCharsets.UTF_8)
      val inputUnit = unitService.inputUnit(specification, text, languageImpl, null)
      val parseResult = syntaxService.parse(inputUnit)

      if (!parseResult.success()) {
        throw new RuntimeException(s"Unsuccessful parse of $specification in language ${languageImpl.id()}.")
      }

      parseResult.ast()
    }

    val ast = parseFile(nablLangImpl)

    // Translate ATerms to Scala DSL
    val labels = toLabels(ast)
    val ordering = toOrdering(ast)
    val wf = toWF(ast)
    val init = toInitRule(ast.getSubterm(1))
    val rules = toRules(ast.getSubterm(1)).map(inlineRecurse)

    Specification(labels, ordering, wf, init :: rules)
  }

  /**
    * Add sort to the Recurse constraints based on the position
    */
  def inlineRecurse(rule: Rule)(implicit signature: Signature) = {
    rule.recurses.foldLeft(rule) {
      case (rule, recurse@CGenRecurse(name, variable, scopes, typ, null, 0)) =>
        val sortOpt = signature.getSort(rule.pattern, variable)
        val sort = sortOpt.getOrElse(throw new IllegalStateException("Could not find sort for " + variable + " in " + rule.pattern))

        rule - recurse + CGenRecurse(name, variable, scopes, typ, sort, 0)
    }
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
  def toWF(term: IStrategoTerm): Regex[Label] = {
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
        (Label('P') *) ~ (Label('I') *)
      case Some(wf) =>
        wf
    }
  }

  /**
    * Convert the regex to our Scala DSL
    */
  def toRegex(term: IStrategoTerm): Regex[Label] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Concat" =>
      Concatenation(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Or" =>
      Union(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "And" =>
      Intersection(toRegex(appl.getSubterm(0)), toRegex(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Closure" =>
      Star(toRegex(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Epsilon" =>
      Epsilon()
    case appl: StrategoAppl if appl.getConstructor.getName == "Empty" =>
      EmptySet()
    case appl: StrategoAppl if appl.getConstructor.getName == "Label" =>
      Character(toString(appl.getSubterm(0)).head)
    case appl: StrategoAppl if appl.getConstructor.getName == "P" =>
      Character('P')
    case appl: StrategoAppl if appl.getConstructor.getName == "I" =>
      Character('I')
  }

  /**
    * Convert the Rules(_) block to an (init) Rule
    */
  def toInitRule(term: IStrategoTerm)(implicit signatures: Signature): Rule = {
    val rules = term.collectAll {
      case appl: IStrategoAppl if appl.getConstructor.getName == "CGenInitRule" =>
        true
      case _ =>
        false
    }

    // TODO: There's not always an init rule, and thus not always a head...

    rules.head match {
      case appl: StrategoAppl =>
        val scopes = toVars(appl.getSubterm(0).getSubterm(0))

        Rule(
          name =
            "Init",
          sort =
            SortVar("y"),
          pattern =
            Var("x"),
          scopes =
            scopes,
          typ =
            toTypeOption(-1, appl.getSubterm(1)),
          constraints =
            toConstraints(-1, appl.getSubterm(2))
        )
    }
  }

  /**
    * Convert the Rules(_) block to a list of Rule
    */
  def toRules(term: IStrategoTerm)(implicit signatures: Signature): List[Rule] = {
    val rules = term.collectAll {
      case appl: IStrategoAppl if appl.getConstructor.getName == "CGenMatchRule" =>
        true
      case appl: IStrategoAppl if appl.getConstructor.getName == "CGenMatchRuleNoConstraint" =>
        true
      case _ =>
        false
    }

    rules.zipWithIndex.map {
      case (appl: IStrategoAppl, index) if appl.getConstructor.getName == "CGenMatchRule" =>
        toRule(index, appl)
      case (appl: IStrategoAppl, index) if appl.getConstructor.getName == "CGenMatchRuleNoConstraint" =>
        toRuleNoConstraint(index, appl)
    }
  }

  /**
    * Turn a CGenRule into a Rule
    */
  def toRule(ruleIndex: Int, term: IStrategoTerm)(implicit signatures: Signature): Rule = term match {
    case appl: StrategoAppl =>
      val name = toRuleName(appl.getSubterm(0))
      val pattern = toPattern(appl.getSubterm(1))
      val scopes = toVars(appl.getSubterm(2).getSubterm(0))
      val typ = toTypeOption(ruleIndex, appl.getSubterm(3))

      Rule(
        name =
          name,
        sort =
          toSort(pattern),
        pattern =
          pattern,
        typ =
          typ,
        scopes =
          scopes,
        constraints =
          toConstraints(ruleIndex, appl.getSubterm(4))
      )
  }

  def toRuleNoConstraint(ruleIndex: Int, term: IStrategoTerm)(implicit signatures: Signature): Rule = term match {
    case appl: StrategoAppl =>
      val name = toRuleName(appl.getSubterm(0))
      val pattern = toPattern(appl.getSubterm(1))
      val scopes = toVars(appl.getSubterm(2).getSubterm(0))
      val typ = toTypeOption(ruleIndex, appl.getSubterm(3))

      Rule(
        name =
          name,
        sort =
          toSort(pattern),
        pattern =
          pattern,
        typ =
          typ,
        scopes =
          scopes,
        constraints =
          Nil
      )
  }

  def toRuleName(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NoName" =>
      "Default"
    case appl: StrategoAppl if appl.getConstructor.getName == "Name" =>
      toString(appl.getSubterm(0))
  }

  // Retrieve the sort for the given pattern from the signatures
  def toSort(pattern: Pattern)(implicit signature: Signature): Sort = pattern match {
    case As(_, term) =>
      toSort(term)
    case _ =>
      val signaturesForPattern = signature.getOperations(pattern)

      if (signaturesForPattern.length > 1) {
        logger.warn(s"Multiple signatures match $pattern ($signaturesForPattern), determining sort possibly wrong.")
      }

      signaturesForPattern.head.target
  }

  // Turn a CGenMatch into a Pattern
  def toPattern(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "As" =>
      As(toPattern(appl.getSubterm(0)).asInstanceOf[Var], toPattern(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), toPatternsList(appl.getSubterm(1)))
    case appl: StrategoAppl if appl.getConstructor.getName == "List" && appl.getSubterm(0).getSubtermCount == 0 =>
      TermAppl("Nil")
    case appl: StrategoAppl if appl.getConstructor.getName == "List" && appl.getSubterm(0).getSubtermCount > 0 =>
      appl.getSubterm(0).getAllSubterms.map(toPattern).foldRight(TermAppl("Nil")) {
        case (pattern, list) =>
          TermAppl("Cons", List(pattern, list))
      }
    case appl: StrategoAppl if appl.getConstructor.getName == "ListTail" =>
      val heads = toPatternsList(appl.getSubterm(0))
      val tail = toPattern(appl.getSubterm(1))

      heads.foldRight(tail) { case (x, acc) =>
        TermAppl("Cons", List(x, acc))
      }
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      Var(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      Var("x" + nameProvider.next)
    case appl: StrategoAppl if appl.getConstructor.getName == "Str" =>
      TermString(toString(appl.getSubterm(0)))
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
      Var(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "TList" =>
      // Convert the list to a list of types
      val list = appl.getSubterm(0).getAllSubterms.toList.map(toType(ruleIndex, _))

      // Reduce the list to a Cons/Nil structure
      list.foldRight(TermAppl("Nil")) { case (x, acc) =>
        TermAppl("Cons", List(x, acc))
      }
    // TListTail([Var("ty")],Var("tys"))
    case appl: StrategoAppl if appl.getConstructor.getName == "TListTail" =>
      // Convert the head list to a list of types
      val headList = appl.getSubterm(0).getAllSubterms.toList.map(toType(ruleIndex, _))

      // Convert the tail to a type
      val tail = toType(ruleIndex, appl.getSubterm(1))

      // Combine both in a Cons/Nil structure
      headList.foldRight(tail) { case (x, acc) =>
        TermAppl("Cons", List(x, acc))
      }
    case appl: StrategoAppl if appl.getConstructor.getName == "Op" =>
      TermAppl(toString(appl.getSubterm(0)), appl.getSubterm(1).getAllSubterms.map(toType(ruleIndex, _)).toList)
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      toName(ruleIndex, appl)
  }

  // Turn a list of NaBL2 terms into a List[Pattern]
  def toVars(term: IStrategoTerm): List[Pattern] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "List" =>
      toVars(appl.getSubterm(0))
    case appl: IStrategoList if appl.isEmpty =>
      Nil
    case appl: IStrategoList =>
      toVar(appl.head()) :: toVars(appl.tail())
  }

  // Turn an NaBL2 term into a Pattern
  def toVar(term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      Var(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Wld" =>
      Var("s" + nameProvider.next)
  }

  // Turn a Stratego name into a Name
  def toName(ruleIndex: Int, term: IStrategoTerm): Pattern = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      Var(toString(appl.getSubterm(0)))
    case appl: StrategoAppl if appl.getConstructor.getName == "Occurrence" =>
      TermAppl("Occurrence", List(
        TermString(toNamespace(appl.getSubterm(0))),
        toPattern(appl.getSubterm(1)),
        TermString(String.valueOf(ruleIndex))
      ))
  }

  // Turn a Stratego namespace into a string. Use "Default" as a default namespace.
  def toNamespace(term: IStrategoTerm): String = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Default" =>
      "Default"
    case appl: StrategoAppl if appl.getConstructor.getName == "All" =>
      "All"
    case appl =>
      toString(appl.getSubterm(0))
  }

  // Turn a Stratego list of constraints into a List[Constraint]
  def toConstraints(ruleIndex: Int, constraint: IStrategoTerm): List[Constraint] = constraint match {
    case list: StrategoList =>
      list.getAllSubterms.flatMap(toConstraint(ruleIndex, _)).toList
  }

  // Turn a NewScopes(...) into List[NewScope]
  def toNewScopes(term: IStrategoTerm): List[NewScope] = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "NewScopes" =>
      appl.getSubterm(0).getAllSubterms.map(toNewScope).toList
  }

  // Turn a Var(x) into a NewScope(Var(x))
  def toNewScope(term: IStrategoTerm): NewScope = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Var" =>
      NewScope(Var(toString(appl.getSubterm(0))))
  }

  def toNames(term: IStrategoTerm): Names = term match {
    case appl: StrategoAppl if appl.getConstructor.getName == "Declarations" =>
      Declarations(toVar(appl.getSubterm(0)), toNamespace(appl.getSubterm(1)))
  }

  /**
    * Turn a constraint into a Constarint. Returns None if the constraint is
    * not supported.
    *
    * @param ruleIndex
    * @param constraint
    * @return
    */
  def toConstraint(ruleIndex: Int, constraint: IStrategoTerm): List[Constraint] = constraint match {
    case appl: StrategoAppl if appl.getConstructor.getName == "CTrue" =>
      List(CTrue())
    case appl: StrategoAppl if appl.getConstructor.getName == "CFalse" =>
      List(CFalse())
    case appl: StrategoAppl if appl.getConstructor.getName == "CGRef" =>
      List(CGRef(toName(ruleIndex, appl.getSubterm(0)), toVar(appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDecl" =>
      List(CGDecl(toVar(appl.getSubterm(1)), toName(ruleIndex, appl.getSubterm(0))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CResolve" =>
      List(CResolve(toName(ruleIndex, appl.getSubterm(0)), toName(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CTypeOf" =>
      List(CTypeOf(toName(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGDirectEdge" =>
      List(CGDirectEdge(toVar(appl.getSubterm(0)), toLabel(appl.getSubterm(1)), toVar(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CEqual" =>
      List(CEqual(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGAssoc" =>
      List(CGAssoc(toName(ruleIndex, appl.getSubterm(0)), toVar(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CAssoc" =>
      List(CAssoc(toName(ruleIndex, appl.getSubterm(0)), toVar(appl.getSubterm(2))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CSubtype" =>
      List(CSubtype(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "FSubtype" =>
      List(FSubtype(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGNamedEdge" =>
      List(CGNamedEdge(toVar(appl.getSubterm(2)), toLabel(appl.getSubterm(1)), toName(ruleIndex, appl.getSubterm(0))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CGenRecurse" =>
      List(CGenRecurse(toRuleName(appl.getSubterm(0)), toPattern(appl.getSubterm(1)), toVars(appl.getSubterm(2).getSubterm(0)), toTypeOption(ruleIndex, appl.getSubterm(3)), null, 0))
    case appl: StrategoAppl if appl.getConstructor.getName == "CInequal" =>
      List(CInequal(toType(ruleIndex, appl.getSubterm(0)), toType(ruleIndex, appl.getSubterm(1))))
    case appl: StrategoAppl if appl.getConstructor.getName == "CDistinct" =>
      List(CDistinct(toNames(appl.getSubterm(0))))
    case appl: StrategoAppl if appl.getConstructor.getName == "NewScopes" =>
      toNewScopes(appl)
    case _ =>
      logger.warn("Constraint not supported by generator: " + constraint)

      Nil
  }

  // Turn IStrategoString into String
  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }
}
