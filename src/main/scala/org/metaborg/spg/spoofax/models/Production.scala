package org.metaborg.spg.spoofax.models

case class Production(sort: Sort, rhs: List[Symbol], attributes: List[Attribute] = Nil, cons: Option[String] = None) {
  def isReject: Boolean =
    attributes.contains(Reject())

  def toSignature: Signature = {
    def rhsToConstType(rhs: Symbol): Option[ConstType] = rhs match {
      case x@SortAppl(_, _) =>
        Some(ConstType(x))
      case Iter(sort: Sort) =>
        Some(ConstType(SortAppl("Iter", List(sort))))
      case IterStar(sort: Sort) =>
        Some(ConstType(SortAppl("IterStar", List(sort))))
      case IterSep(sort: Sort, _) =>
        Some(ConstType(SortAppl("Iter", List(sort))))
      case IterStarSep(sort: Sort, _) =>
        Some(ConstType(SortAppl("IterStar", List(sort))))
      case Opt(sort: Sort) =>
        Some(ConstType(SortAppl("Option", List(sort))))
      case _ =>
        None
    }

    cons match {
      case Some(name) =>
        OpDecl(name, FunType(rhs.flatMap(rhsToConstType), ConstType(sort)))
      case None =>
        OpDeclInj(FunType(rhs.flatMap(rhsToConstType), ConstType(sort)))
    }
  }
}

// Attributes
abstract class Attribute

case class Reject() extends Attribute
