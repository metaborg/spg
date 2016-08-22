package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.spoofax.models._
import nl.tudelft.fragments.Main
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoString, IStrategoTerm}
import nl.tudelft.fragments.spoofax.SpoofaxScala._

object Productions {
  val s = Main.spoofax

  def read(sdfPath: String, productionsPath: String) = {
    val nablImpl = Utils.loadLanguage(sdfPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(productionsPath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)

    // Translate ATerms to Scala DSL
    toProductions(parseResult.ast())
  }

  def toProductions(term: IStrategoTerm): List[Production] = {
    val productionTerms = term.collectAll {
      case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProduction" =>
        true
      case _ =>
        false
    }

    productionTerms.map(toProduction)
  }

  // SdfProduction(SortDef("Integernumber"), Rhs([Sort("Digitsequence")]), _)
  def toProduction(term: IStrategoTerm): Production = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProduction" =>
      Production(toSort(appl.getSubterm(0)), toRhs(appl.getSubterm(1)))
  }
  
  def toSort(term: IStrategoTerm): Sort = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SortDef" || appl.getConstructor.getName == "Sort" =>
      Sort(toString(term.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Layout" =>
      Sort("LAYOUT")
  }

  def toRhs(term: IStrategoTerm): List[Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Rhs" =>
      appl.getSubterm(0).getAllSubterms.toList.map(toSymbol)
  }

  def toSymbol(term: IStrategoTerm): Symbol = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Sort" =>
      toSort(term)
    case appl: IStrategoAppl if appl.getConstructor.getName == "Lit" =>
      Lit(toString(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Opt" =>
      Opt(toSymbol(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Iter" =>
      Iter(toSymbol(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterStar" =>
      IterStar(toSymbol(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "CharClass" =>
      toCharClass(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Comp" =>
      toCharClass(term)
  }

  def toCharClass(term: IStrategoTerm): CharClass = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Comp" =>
      Comp(toCharClass(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Simple" =>
      Simple(toPresence(term.getSubterm(0)): _*)
  }

  def toPresence(term: IStrategoTerm): List[CharacterRange] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Absent" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Present" =>
      toCharacterRanges(appl.getSubterm(0))
  }

  def toCharacterRanges(term: IStrategoTerm): List[CharacterRange] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Conc" =>
      toCharacterRange(term.getSubterm(0)) :: toCharacterRanges(term.getSubterm(1))
    case _ =>
      List(toCharacterRange(term))
  }

  def toCharacterRange(term: IStrategoTerm): CharacterRange = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Range" =>
      Range(toCharacter(term.getSubterm(0)), toCharacter(term.getSubterm(1)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Short" =>
      toCharacter(term)
  }

  def toCharacter(term: IStrategoTerm): Character = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Short" =>
      Short(unescape(toString(term.getSubterm(0))).head)
  }

  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }

  def unescape(s: String): String =
    s
      .replace("\\+", "+")
      .replace("\\-", "-")
      .replace("\\_", "_")
      .replace("\\t", "t")
      .replace("\\n", "n")
}
