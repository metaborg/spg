package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.Strategy2
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}

object Signatures {
  val s = Strategy2.spoofax

  /**
    * Parse the signatures
    */
  def read(strategoPath: String, signaturePath: String) = {
    val nablImpl = Utils.loadLanguage(strategoPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(signaturePath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)

    // Translate ATerms to Scala DSL
    toConstructors(parseResult.ast().getSubterm(1).asInstanceOf[IStrategoList].tail().head().getSubterm(0).asInstanceOf[IStrategoList].head().getSubterm(0))
  }

  def toConstructors(term: IStrategoTerm): List[Decl] = term match {
    case list: IStrategoList =>
      if (!list.isEmpty) {
        toConstructor(list.head()) :: toConstructors(list.tail())
      } else {
        Nil
      }
  }

  def toConstructor(term: IStrategoTerm): Decl = term match {
    case appl: IStrategoAppl =>
      appl.getConstructor.getName match {
        case "OpDecl" =>
          OpDecl(toString(appl.getSubterm(0)), toType(appl.getSubterm(1)))
        case "OpDeclInj" =>
          OpDeclInj(toType(appl.getSubterm(0)))
      }
  }

  def toTypes(term: IStrategoTerm): List[Type] = term match {
    case list: IStrategoList =>
      if (!list.isEmpty) {
        toType(list.head()) :: toTypes(list.tail())
      } else {
        Nil
      }
  }

  def toType(term: IStrategoTerm): Type = term match {
    case appl: IStrategoAppl =>
      appl.getConstructor.getName match {
        case "FunType" =>
          FunType(toTypes(appl.getSubterm(0)), toType(appl.getSubterm(1)))
        case "ConstType" =>
          ConstType(toSort(appl.getSubterm(0)))
      }
  }

  def toSorts(term: IStrategoTerm): List[Sort] = term match {
    case list: IStrategoList =>
      if (!list.isEmpty) {
        toSort(list.head()) :: toSorts(list.tail())
      } else {
        Nil
      }
  }

  def toSort(term: IStrategoTerm): Sort = term match {
    case appl: IStrategoAppl =>
      appl.getConstructor.getName match {
        case "SortNoArgs" =>
          SortNoArgs(toString(appl.getSubterm(0)))
        case "Sort" =>
          SortArgs(toString(appl.getSubterm(0)), toSorts(appl.getSubterm(1)))
      }
  }

  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }

  abstract class Decl
  case class OpDecl(name: String, typ: Type) extends Decl
  case class OpDeclInj(typ: Type) extends Decl

  abstract class Type
  case class FunType(children: List[Type], result: Type) extends Type
  case class ConstType(sort: Sort) extends Type

  abstract class Sort
  case class SortNoArgs(name: String) extends Sort
  case class SortArgs(name: String, sorts: List[Sort]) extends Sort
}
