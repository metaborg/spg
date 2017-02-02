package org.metaborg.spg.core.spoofax

import com.typesafe.scalalogging.LazyLogging
import org.metaborg.core.language.{ILanguageImpl, LanguageService => BaseLanguageService}
import org.metaborg.core.project.IProject
import org.metaborg.core.resource.ResourceService
import org.metaborg.spg.core.spoofax.models._
import org.metaborg.spoofax.core.syntax.SyntaxFacet

import scala.collection.JavaConverters._

class LanguageService(val baseLanguageService: BaseLanguageService, val resourceSerivce: ResourceService, val productionService: ProductionService, val specificationService: SpecificationService, val printerService: PrinterService) extends LazyLogging {
  /**
    * Load a language for generation.
    *
    * @param templateLangImpl
    * @param nablLangImpl
    * @param lutLangImpl
    * @param project
    * @param semanticsPath
    * @return
    */
  def load(templateLangImpl: ILanguageImpl, nablLangImpl: ILanguageImpl, lutLangImpl: ILanguageImpl, project: IProject, semanticsPath: String): Language = {
    logger.info("Loading productions")
    val productions = productionService.read(templateLangImpl, resourceSerivce.resolve(project.location(), s"syntax/${lutLangImpl.belongsTo().name()}.sdf3"))

    logger.info("Computing signatures")
    val signatures = Signatures(defaultSignatures ++ productions.map(_.toSignature))

    logger.info("Loading static semantics")
    val specification = specificationService.read(nablLangImpl, resourceSerivce.resolve(project.location(), semanticsPath))(signatures)

    logger.info("Constructing printer")
    val printer = printerService.getPrinter(lutLangImpl, project)

    logger.info("Read start symbols")
    val start = startSymbols(lutLangImpl)

    Language(productions, signatures, specification, printer, start, lutLangImpl)
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
    * These signatures are defined in the standard library and do not appear in
    * the signature file. Hence, we add them ourselves.
    *
    * The last two signatures define the sort Iter(a) and IterStar(a):
    *  - Iter(a) is a list with at least one element. This is not supported in
    * Stratego.
    *  - IterStar(a) is a list with zero or more elements. It is the same as
    * List(a), so we define an injection to it.
    *
    * @return
    */
  def defaultSignatures: List[Signature] = List(
    // Cons : a * Lits(a) -> List(a)
    OpDecl("Cons", FunType(
      List(
        ConstType(SortVar("a")),
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("List", List(SortVar("a"))))
    )),

    // Nil : List(a)
    OpDecl("Nil", ConstType(
      SortAppl("List", List(SortVar("a")))
    )),

    // Some : Option(a)
    OpDecl("Some", FunType(
      List(
        ConstType(SortVar("a"))
      ),
      ConstType(SortAppl("Option", List(SortVar("a"))))
    )),

    // None : Option(a)
    OpDecl("None", ConstType(
      SortAppl("Option", List(SortVar("a")))
    )),

    // Conss : a * List(a) -> Iter(a)
    OpDecl("Conss", FunType(
      List(
        ConstType(SortVar("a")),
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("Iter", List(SortVar("a"))))
    )),

    // : List(a) -> IterStar(a)
    OpDeclInj(FunType(
      List(
        ConstType(SortAppl("List", List(SortVar("a"))))
      ),
      ConstType(SortAppl("IterStar", List(SortVar("a"))))
    ))
  )
}
