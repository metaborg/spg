package org.metaborg.spg.core.spoofax

import com.google.inject.Inject
import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.ILanguageImpl
import org.metaborg.core.project.IProject
import org.metaborg.core.resource.ResourceService
import org.metaborg.spg.core.nabl.NablService
import org.metaborg.spg.core.sdf.{SdfService, Sort, SortAppl, SortVar}
import org.metaborg.spg.core.stratego.{Constructor, Injection, Operation, Signature}
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
    val name = lutLangImpl.belongsTo().name
    val signatures = Signature(LanguageService.defaultSignatures ++ grammar.effectiveGrammar(name).toConstructors)

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
}

object LanguageService {
  /**
    * The constructors for sorts List(a) and Option(a) are implicit in Spoofax.
    * This method lists these implicit constructors.
    *
    * Moreover, Spoofax uses sort List(a) for both empty and non-empty lists.
    * However, we do not want to generate empty lists when non-empty lists are
    * required. We define sorts Iter(a) and IterStar(a) for non-empty lists and
    * empty lists, respectively.
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

    // IterCons : a * List(a) -> Iter(a)
    //Operation("IterCons", List(SortVar("a"), SortAppl("List", List(SortVar("a")))), SortAppl("Iter", List(SortVar("a")))),

    // Cons : a * List(a) -> Iter(a)
    Operation("Cons", List(SortVar("a"), SortAppl("List", List(SortVar("a")))), SortAppl("Iter", List(SortVar("a")))),

    // : List(a) -> IterStar(a)
    Injection(SortAppl("List", List(SortVar("a"))), SortAppl("IterStar", List(SortVar("a"))))

    // We made amb part of the signature so we can get its sort. But if we do this, then the sentence generator will generate amb nodes. We should not have amb nodes in the tree to begin with...
    // amb : List(a) -> a
    //Operation("amb", List(SortAppl("List", List(SortVar("a")))), SortVar("a"))
  )
}
