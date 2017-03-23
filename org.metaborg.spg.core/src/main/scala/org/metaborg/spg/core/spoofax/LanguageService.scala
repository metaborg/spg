package org.metaborg.spg.core.spoofax

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.IProject
import org.metaborg.core.resource.ResourceService
import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spoofax.core.syntax.SyntaxFacet

import scala.collection.JavaConverters._

class LanguageService @Inject()(val resourceSerivce: ResourceService, val sdfService: SdfService, val specificationService: NablService, val printerService: PrinterService) extends LazyLogging {
  /**
    * Load a language for generation.
    *
    * This method will load all SDF files and all NaBL2 files independent of
    * whether they are imported.
    *
    * @param templateLangImpl
    * @param nablLangImpl
    * @param lutLangImpl
    * @param project
    * @return
    */
  def load(templateLangImpl: ILanguageImpl, nablLangImpl: ILanguageImpl, lutLangImpl: ILanguageImpl, project: IProject): Language = {
    logger.trace("Loading productions")
    val grammar = sdfService.read(templateLangImpl, project)

    logger.trace("Computing signatures")
    val signatures = Signature(defaultSignatures ++ grammar.toConstructors)

    logger.trace("Loading static semantics")
    val specification = specificationService.read(nablLangImpl, project)(signatures)

    logger.trace("Constructing printer")
    val printer = printerService.getPrinter(lutLangImpl, project)

    logger.trace("Read start symbols")
    val start = startSymbols(lutLangImpl)

    Language(grammar, signatures, specification, printer, start, lutLangImpl)
  }

  /**
    * Compute all start symbols for the given language.
    *
    * @param languageImpl
    * @return
    */
  def startSymbols(languageImpl: ILanguageImpl): Set[Sort] = {
    languageImpl
      .facets(classOf[SyntaxFacet]).asScala
      .flatMap(_.startSymbols.asScala)
      .map(SortAppl(_))
      .toSet
  }

  /**
    * Spoofax leaves the constructors for List(a) and Option(a) out of the
    * generated signature file, so we add them ourselves.
    *
    * Spoofax's List(a) does not distinguish between empty and non-empty lists.
    * We define Iter(a) and IterStar(a) for non-empty and empty lists,
    * respectively.
    *
    * TODO: Drop IterStar(a), since it is the same as List(a). We only need to add Iter(a).
    *
    * @return
    */
  lazy val defaultSignatures: List[Constructor] = List(
    // Cons : a * List(a) -> List(a)
    Operation("Cons", List(SortVar("a"), SortAppl("List", List(SortVar("a")))), SortAppl("List", List(SortVar("a")))),

    // Nil : List(a)
    Operation("Nil", Nil, SortAppl("List", List(SortVar("a")))),

    // Some : a -> Option(a)
    Operation("Some", List(SortVar("a")), SortAppl("Option", List(SortVar("a")))),

    // None : Option(a)
    Operation("None", Nil, SortAppl("Option", List(SortVar("a")))),

    // Conss : a * List(a) -> Iter(a)
    Operation("Conss", List(SortVar("a"), SortAppl("List", List(SortVar("a")))), SortAppl("Iter", List(SortVar("a")))),

    // : List(a) -> IterStar(a)
    Injection(SortAppl("List", List(SortVar("a"))), SortAppl("IterStar", List(SortVar("a"))))
  )
}
