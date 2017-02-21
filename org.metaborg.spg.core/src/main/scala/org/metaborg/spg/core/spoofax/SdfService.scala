package org.metaborg.spg.core.spoofax

import java.nio.charset.StandardCharsets

import com.google.inject.Inject
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.IProject
import org.metaborg.core.resource.IResourceService
import org.metaborg.spg.core.spoofax.SpoofaxScala._
import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService
import org.metaborg.util.resource.FileSelectorUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoString, IStrategoTerm}

/**
  * The SDF service loads all SDF production from a Spoofax project or a single
  * SDF file.
  *
  * @param resourceService
  * @param unitService
  * @param syntaxService
  */
class SdfService @Inject()(val resourceService: IResourceService, val unitService: ISpoofaxUnitService, val syntaxService: ISpoofaxSyntaxService) {
  /**
    * Read all SDF files in the given project and collect the productions.
    *
    * @return
    */
  def read(templateLangImpl: ILanguageImpl, project: IProject): List[Production] = {
    val fileSelector = FileSelectorUtils.extension("sdf3")
    val files = project.location().findFiles(fileSelector).toList

    files.flatMap(read(templateLangImpl, _))
  }

  /**
    * Parse an SDF file and extract a list of productions.
    *
    * @param templateLangImpl
    * @param syntax
    * @return
    */
  def read(templateLangImpl: ILanguageImpl, syntax: FileObject): List[Production] = {
    def parseFile(languageImpl: ILanguageImpl): IStrategoTerm = {
      val text = IOUtils.toString(syntax.getContent.getInputStream, StandardCharsets.UTF_8)
      val inputUnit = unitService.inputUnit(syntax, text, languageImpl, null)
      val parseResult = syntaxService.parse(inputUnit)

      if (!parseResult.success()) {
        throw new RuntimeException(s"Unsuccessful parse of $syntax in language ${languageImpl.id()}.")
      }

      parseResult.ast()
    }

    val ast = parseFile(templateLangImpl)

    toProductions(ast)
  }

  private def toProductions(term: IStrategoTerm): List[Production] = {
    val productionTerms = term.collectAll {
      case appl: IStrategoAppl =>
        appl.getConstructor.getName == "SdfProduction" ||
          appl.getConstructor.getName == "SdfProductionWithCons" ||
          appl.getConstructor.getName == "TemplateProduction" ||
          appl.getConstructor.getName == "TemplateProductionWithCons"
      case _ =>
        false
    }

    productionTerms.map(toProduction)
  }

  private def toProduction(term: IStrategoTerm): Production = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProduction" =>
      Production(toSort(appl.getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "SdfProductionWithCons" =>
      Production(toSort(appl.getSubterm(0).getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)), Some(toConstructor(appl.getSubterm(0).getSubterm(1))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "TemplateProduction" =>
      Production(toSort(appl.getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "TemplateProductionWithCons" =>
      Production(toSort(appl.getSubterm(0).getSubterm(0)), toRhs(appl.getSubterm(1)), toAttrs(appl.getSubterm(2)), Some(toConstructor(appl.getSubterm(0).getSubterm(1))))
  }

  private def toAttrs(term: IStrategoTerm): List[Attribute] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Attrs" =>
      appl.getSubterm(0).getAllSubterms.toList.flatMap(toAttr)
    case appl: IStrategoAppl if appl.getConstructor.getName == "NoAttrs" =>
      Nil
  }

  private def toAttr(term: IStrategoTerm): Option[Attribute] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Reject" =>
      Some(Reject())
    case _ =>
      None
  }


  private def toConstructor(term: IStrategoTerm): String = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Constructor" =>
      toString(term.getSubterm(0))
  }

  private def toSort(term: IStrategoTerm): Sort = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "SortDef" || appl.getConstructor.getName == "Sort" =>
      SortAppl(toString(term.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Layout" =>
      SortAppl("LAYOUT")
  }

  private def toRhs(term: IStrategoTerm): List[models.Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Rhs" =>
      appl.getSubterm(0).getAllSubterms.toList.map(toSymbol)

    // For template productions
    case appl: IStrategoAppl if appl.getConstructor.getName == "Template" =>
      appl.getSubterm(0).getAllSubterms.toList.flatMap(toLine)
    case appl: IStrategoAppl if appl.getConstructor.getName == "TemplateSquare" =>
      term.getSubterm(0).getAllSubterms.toList.flatMap(toLine)
  }

  private def toLine(term: IStrategoTerm): List[models.Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Line" =>
      term.getSubterm(0).getAllSubterms.toList.flatMap(toTemplateSymbol)
  }

  // Many template symbols are ignored, since they are not important for generation.
  private def toTemplateSymbol(term: IStrategoTerm): List[models.Symbol] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Layout" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Escape" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "String" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Squared" =>
      toTemplateSymbol(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Angled" =>
      toTemplateSymbol(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Placeholder" =>
      toTemplateSymbol(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterStarSep" =>
      List(IterStarSep(toSort(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterStar" =>
      List(IterStar(toSort(appl.getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Iter" =>
      List(Iter(toSort(appl.getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "IterSep" =>
      List(IterSep(toSort(appl.getSubterm(0)), toString(appl.getSubterm(1).getSubterm(0))))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Sort" =>
      List(toSymbol(appl))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Opt" =>
      List(toSymbol(appl))
  }

  private def toSymbol(term: IStrategoTerm): models.Symbol = term match {
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
    case appl: IStrategoAppl if appl.getConstructor.getName == "Alt" =>
      Alt(toSymbol(appl.getSubterm(0)), toSymbol(appl.getSubterm(1)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "CharClass" =>
      toCharClass(appl.getSubterm(0))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Comp" =>
      toCharClass(term)
  }

  private def toCharClass(term: IStrategoTerm): CharClass = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Comp" =>
      Comp(toCharClass(appl.getSubterm(0)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Simple" =>
      Simple(toPresence(term.getSubterm(0)): _*)
  }

  private def toPresence(term: IStrategoTerm): List[CharacterRange] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Absent" =>
      Nil
    case appl: IStrategoAppl if appl.getConstructor.getName == "Present" =>
      toCharacterRanges(appl.getSubterm(0))
  }

  private def toCharacterRanges(term: IStrategoTerm): List[CharacterRange] = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Conc" =>
      toCharacterRange(term.getSubterm(0)) :: toCharacterRanges(term.getSubterm(1))
    case _ =>
      List(toCharacterRange(term))
  }

  private def toCharacterRange(term: IStrategoTerm): CharacterRange = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Range" =>
      Range(toCharacter(term.getSubterm(0)), toCharacter(term.getSubterm(1)))
    case appl: IStrategoAppl if appl.getConstructor.getName == "Short" =>
      toCharacter(term)
  }

  private def toCharacter(term: IStrategoTerm): Character = term match {
    case appl: IStrategoAppl if appl.getConstructor.getName == "Short" =>
      Short(unescape(toString(term.getSubterm(0))).head)
  }

  private def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      unescape(string.stringValue())
  }

  private def unescape(s: String): String =
    s
      .replace("\\ ", " ") // 2-char string `\ ` becomes 1-char string ` `
      .replace("\\t", "\t") // 2-char string `\t` becomes 1-char string `\t`
      .replace("\\n", "\n") // 2-char string `\n` becomes 1-char string `\n`
      .replace("\\r", "\r") // 2-char string `\r` becomes 1-char string `\r`
      .replace("\\*", "*") // 2-char string `\*` becomes 1-char string `*`
      .replace("\\\"", "\"") // 2-char string `\"` becomes 1-char string `"`
      .replace("\\\\", "\\") // 2-char string `\\` becomes 1-char string `\`
      .replace("\\_", "_") // 2-char string `\_` becomes 1-char string `_`
      .replace("\\^", "^") // 2-char string `\^` becomes 1-char string `^`
      .replace("\\+", "+") // 2-char string `\+` becomes 1-char string `+`
      .replace("\\-", "-") // 2-char string `\-` becomes 1-char string `-`
      .replace("\\<", "<") // 2-char string `\<` becomes 1-char string `<`
      .replace("\\>", ">") // 2-char string `\>` becomes 1-char string `>`
}