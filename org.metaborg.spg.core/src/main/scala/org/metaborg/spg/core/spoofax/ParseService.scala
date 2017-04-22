package org.metaborg.spg.core.spoofax

import java.nio.charset.StandardCharsets.UTF_8

import com.google.inject.Inject
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.resource.ResourceService
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService
import org.metaborg.spoofax.core.unit.{ISpoofaxInputUnit, ISpoofaxParseUnit, ISpoofaxUnitService}
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoTerm}

class ParseService @Inject()(val unitService: ISpoofaxUnitService, val syntaxService: ISpoofaxSyntaxService, val resourceService: ResourceService) {
  /**
    * Parse content of a FileObject in given language implementation.
    *
    * @param languageImpl
    * @param fileObject
    * @return
    */
  def parse(languageImpl: ILanguageImpl, fileObject: FileObject): ISpoofaxParseUnit = {
    val contents = IOUtils.toString(fileObject.getContent.getInputStream, UTF_8)

    parse(languageImpl, contents)
  }

  /**
    * Parse given contents in given language implementation.
    *
    * @param languageImpl
    * @param contents
    * @return
    */
  def parse(languageImpl: ILanguageImpl, contents: String): ISpoofaxParseUnit = {
    val inputUnit = unitService.inputUnit(contents, languageImpl, languageImpl)

    parse(languageImpl, inputUnit)
  }

  /**
    * Parse given input unit in given language implementation.
    *
    * @param languageImpl
    * @param inputUnit
    * @return
    */
  def parse(languageImpl: ILanguageImpl, inputUnit: ISpoofaxInputUnit): ISpoofaxParseUnit = {
    val parseResult = syntaxService.parse(inputUnit)

    if (!parseResult.success()) {
      throw new RuntimeException(s"Unsuccessful parse of $inputUnit in language ${languageImpl.id()}.")
    }

    parseResult
  }

  /**
    * Check if the given parse unit is ambiguous.
    *
    * @param parseUnit
    * @return
    */
  def isAmbiguous(parseUnit: ISpoofaxParseUnit): Boolean = {
    isAmbiguous(parseUnit.ast())
  }

  /**
    * Check if the given Stratego term is ambiguous.
    *
    * @param term
    * @return
    */
  def isAmbiguous(term: IStrategoTerm): Boolean = {
    term match {
      case appl: IStrategoAppl =>
        if (appl.getConstructor.getName == "amb") {
          return true
        }
      case _ =>
        // Noop
    }

    term.getAllSubterms.exists(isAmbiguous)
  }
}
