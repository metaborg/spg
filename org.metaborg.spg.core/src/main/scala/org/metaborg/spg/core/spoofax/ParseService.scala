package org.metaborg.spg.core.spoofax

import java.nio.charset.StandardCharsets.UTF_8

import com.google.inject.Inject
import org.apache.commons.io.IOUtils
import org.apache.commons.vfs2.FileObject
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.spoofax.core.syntax.ISpoofaxSyntaxService
import org.metaborg.spoofax.core.unit.{ISpoofaxInputUnit, ISpoofaxUnitService}
import org.spoofax.interpreter.terms.IStrategoTerm

class ParseService @Inject()(val unitService: ISpoofaxUnitService, val syntaxService: ISpoofaxSyntaxService) {
  /**
    * Parse content of a FileObject in given language implementation.
    *
    * @param languageImpl
    * @param fileObject
    * @return
    */
  def parse(languageImpl: ILanguageImpl, fileObject: FileObject): IStrategoTerm = {
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
  def parse(languageImpl: ILanguageImpl, contents: String): IStrategoTerm = {
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
  def parse(languageImpl: ILanguageImpl, inputUnit: ISpoofaxInputUnit): IStrategoTerm = {
    val parseResult = syntaxService.parse(inputUnit)

    if (!parseResult.success()) {
      throw new RuntimeException(s"Unsuccessful parse of $inputUnit in language ${languageImpl.id()}.")
    }

    parseResult.ast()
  }
}
