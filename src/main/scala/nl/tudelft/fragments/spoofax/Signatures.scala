package nl.tudelft.fragments.spoofax

import nl.tudelft.fragments.{Main, Sort, SortAppl, SortVar}
import org.apache.commons.io.IOUtils
import org.spoofax.interpreter.terms.{IStrategoAppl, IStrategoList, IStrategoString, IStrategoTerm}

object Signatures {
  val s = Main.spoofax

  // Parse the signatures
  def read(strategoPath: String, signaturePath: String) = {
    val nablImpl = Utils.loadLanguage(strategoPath)

    // Get content to parse and build inputUnit
    val file = s.resourceService.resolve(signaturePath)
    val text = IOUtils.toString(file.getContent.getInputStream)
    val inputUnit = s.unitService.inputUnit(text, nablImpl, null)

    // Parse
    val parseResult = s.syntaxService.parse(inputUnit)

    // Translate ATerms to Scala DSL (TODO: Make more fault-tolerant. If we don't have imports, then it may not be ast[1].tail.head[0])
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

  // Turn Stratego list of sorts into List[Sort]
  def toSorts(term: IStrategoTerm): List[Sort] = term match {
    case list: IStrategoList =>
      if (!list.isEmpty) {
        toSort(list.head()) :: toSorts(list.tail())
      } else {
        Nil
      }
  }

  // Turn Stratego SortNoArgs or Sort into SortAppl
  def toSort(term: IStrategoTerm): Sort = term match {
    case appl: IStrategoAppl =>
      appl.getConstructor.getName match {
        case "SortNoArgs" =>
          SortAppl(toString(appl.getSubterm(0)))
        case "Sort" =>
          SortAppl(toString(appl.getSubterm(0)), toSorts(appl.getSubterm(1)))
        case "SortVar" =>
          SortVar(toString(appl.getSubterm(0)))
      }
  }

  def toString(term: IStrategoTerm): String = term match {
    case string: IStrategoString =>
      string.stringValue()
  }

  // Injections for given sort
  def injections(sort: Sort)(implicit signatures: List[Decl]): Set[Sort] = {
    signatures.flatMap {
      case OpDeclInj(FunType(List(ConstType(x)), ConstType(`sort`))) =>
        List(x)
      case _ =>
        Nil
    }.toSet
  }

  // Reflexive, transitive closure of injections for given sort
  def injectionsClosure(sorts: Set[Sort])(implicit signatures: List[Decl]): Set[Sort] = {
    val newSorts = sorts.flatMap(injections) ++ sorts

    if (newSorts == sorts) {
      sorts
    } else {
      injectionsClosure(newSorts)
    }
  }

  abstract class Decl
  case class OpDecl(name: String, typ: Type) extends Decl
  case class OpDeclInj(typ: Type) extends Decl

  abstract class Type
  case class FunType(children: List[Type], result: Type) extends Type
  case class ConstType(sort: Sort) extends Type
}
