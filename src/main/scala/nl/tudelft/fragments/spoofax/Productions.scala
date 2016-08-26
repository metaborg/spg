package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.Main
import nl.tudelft.fragments.spoofax.SpoofaxScala._
import nl.tudelft.fragments.spoofax.models._
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoString, IStrategoTerm}

object Productions {
  val s = Main.spoofax

  def read(sdfPath: String, productionsPath: String): List[Production] = {
    val nablImpl = Utils.loadLanguage(sdfPath)
    val ast = Utils.parseFile(nablImpl, productionsPath)

    toProductions(ast)
  }

  def toProductions(term: IStrategoTerm): List[Production] = {
    val productionTerms = term.collectAll {
      case appl: IStrategoAppl =>
        appl.getConstructor.getName == "SdfProduction" ||
        appl.getConstructor.getName == "SdfProductionWithCons" ||
        appl.getConstructor.getName == "TemplateProductionWithCons"
      case _ =>
        false
    }

    productionTerms.map(toProduction)
  }

  def toProduction(term: IStrategoTerm): Production = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProduction" =>
      Production(toSort(appl.getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProductionWithCons" =>
      Production(toSort(appl.getSubterm(0).getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)), Some(toConstructor(appl.getSubterm(0).getSubterm(1))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "TemplateProductionWithCons" =>
      Production(toSort(appl.getSubterm(0).getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)), Some(toConstructor(appl.getSubterm(0).getSubterm(1))))
  }

  def toAttrs(term: IStrategoTerm): List[Attribute] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Attrs" =>
      appl.getSubterm(0).getAllSubterms.toList.flatMap(toAttr)
    case appl: IStrategoAppl if appl.getConstructor.getName == "NoAttrs" =>
      Nil
  }

  def toAttr(term: IStrategoTerm): Option[Attribute] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Reject" =>
      Some(Reject())
    case _ =>
      None
  }


  def toConstructor(term: IStrategoTerm): String = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Constructor" =>
      toString(term.getSubterm(0))
  }

  def toSort(term: IStrategoTerm): Sort = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SortDef" || appl.getConstructor.getName == "Sort" =>
      SortAppl(toString(term.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Layout" =>
      SortAppl("LAYOUT")
  }

  def toRhs(term: IStrategoTerm): List[Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Rhs" =>
      appl.getSubterm(0).getAllSubterms.toList.map(toSymbol)

    // For template productions
    case appl: IStrategoAppl if appl.getConstructor.getName == "Template" =>
      appl.getSubterm(0).getAllSubterms.toList.flatMap(toLine)
    case appl: IStrategoAppl if appl.getConstructor.getName == "TemplateSquare" =>
      term.getSubterm(0).getAllSubterms.toList.flatMap(toLine)
  }

  def toLine(term: IStrategoTerm): List[Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Line" =>
      term.getSubterm(0).getAllSubterms.toList.flatMap(toTemplateSymbol)
  }

  // Many template symbols are ignored, since they are not important for generation.
  def toTemplateSymbol(term: IStrategoTerm): List[Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Layout" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Escape" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "String" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Angled" =>
      toTemplateSymbol(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Placeholder" =>
      toTemplateSymbol(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterStarSep" =>
      List(IterStarSep(toSort(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterStar" =>
      List(toSort(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Iter" =>
      List(toSort(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterSep" =>
      List(IterSep(toSort(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Sort" =>
      List(toSymbol(appl))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Opt" =>
      List(toSymbol(appl))
  }

  def toSymbol(term: IStrategoTerm): Symbol = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Sort" =>
      toSort(term)
    case appl: IStrategoAppl if appl.getConstructor.getName == "Lit" =>
      Lit(toString(appl.getSubterm(0)).tail.init)
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
      .replace("\\r", "r")
      .replace("\\n", "n")
      .replace("\\<", "<")
      .replace("\\>", ">")
}
