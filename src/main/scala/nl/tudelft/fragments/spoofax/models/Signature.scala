package nl.tudelft.fragments.spoofax.models

import nl.tudelft.fragments.{As, Pattern, TermAppl, Var}

case class Signatures(list: List[Signature]) {
  /**
    * Get signatures for the given pattern based on its constructor name and arity.
    *
    * @param pattern
    * @return
    */
  def forPattern(pattern: Pattern): List[OpDecl] = pattern match {
    case termAppl: TermAppl =>
      list
        .filter(_.isInstanceOf[OpDecl])
        .asInstanceOf[List[OpDecl]]
        .filter(_.name == termAppl.cons)
        .filter(_.arity == termAppl.arity)
    case _ =>
      Nil
  }

  /**
    * Get the sort of p2 for its occurrence in p1. If p2 occurs multiple times
    * in p1, the sort for the first occurrence is returned.
    *
    * @param p1
    * @param p2
    * @param sort
    * @return
    */
  def sortForPattern(p1: Pattern, p2: Pattern, sort: Option[Sort] = None): Option[Sort] = (p1, p2) match {
    case (_, _) if p1 == p2 =>
      sort
    case (As(Var(n1), _), Var(n2)) if n1 == n2 =>
      sort
    case (As(Var(n1), term1), term2) =>
      sortForPattern(term1, term2)
    case (termAppl@TermAppl(_, children), _) =>
      // If `forPattern(p1) == Nil` then we were unable to identify a signature for p1
      val signature = forPattern(p1).head

      val sorts = signature.typ match {
        case FunType(children, _) =>
          children
        case ConstType(_) =>
          Nil
      }

      (children, sorts).zipped.foldLeft(Option.empty[Sort]) {
        case (Some(x), _) =>
          Some(x)
        case (_, (child, ConstType(sort))) =>
          sortForPattern(child, p2, Some(sort))
        case (_, (child, FunType(_, ConstType(sort)))) =>
          sortForPattern(child, p2, Some(sort))
        case (None, _) =>
          ???
      }
    case _ =>
      None
  }
}

abstract class Signature

case class OpDecl(name: String, typ: Type) extends Signature {
  /**
    * The arity of a signature is the number of arguments it takes.
    *
    * @return
    */
  def arity = typ match {
    case FunType(children, _) =>
      children.length
    case ConstType(_) =>
      0
  }

  /**
    * Get the sort (result type) for this signature
    *
    * @return
    */
  def sort = typ match {
    case FunType(_, ConstType(sort)) =>
      sort
    case ConstType(sort) =>
      sort
  }

  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    s"$name : $typ"
}

case class OpDeclInj(typ: Type) extends Signature {
  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    s"$typ"
}

abstract class Type

case class FunType(children: List[Type], result: Type) extends Type {
  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    s"${children.mkString(" * ")} -> $result"
}

case class ConstType(sort: Sort) extends Type {
  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    sort.toString
}
