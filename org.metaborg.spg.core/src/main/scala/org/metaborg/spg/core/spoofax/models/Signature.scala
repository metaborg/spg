package org.metaborg.spg.core.spoofax.models

import org.metaborg.spg.core.terms.{As, Pattern, TermAppl, Var}

case class Signatures(list: List[Signature]) {
  /**
    * Get all operations (constructors) for the given sort.
    *
    * TODO: The results should be cached. They cannot be precompute (due to sort variables/unification), but they can be memoized...
    *
    * @param sort
    * @return
    */
  def forSort(sort: Sort): List[OpDecl] = {
    list.flatMap {
      case c@OpDecl(_, ConstType(s)) if s.unify(sort).isDefined =>
        List(c.substituteSort(s.unify(sort).get))
      case c@OpDecl(_, FunType(_, ConstType(s))) if s.unify(sort).isDefined =>
        List(c.substituteSort(s.unify(sort).get))
      case OpDeclInj(FunType(List(ConstType(childSort)), ConstType(s))) if s.unify(sort).isDefined =>
        forSort(childSort.substituteSort(s.unify(sort).get))
      case _ =>
        Nil
    }
  }

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
    * Try to detect the signature for a given pattern based on its constructor
    * name and its children.
    *
    * @param pattern
    * @return
    */
  def detectSignature(pattern: Pattern): List[OpDecl] = {
    ???
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

abstract class Signature {
  def substituteSort(binding: Map[SortVar, Sort]): Signature
}

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
    * Substitute sort variables in this constructor.
    *
    * @param binding
    * @return
    */
  override def substituteSort(binding: Map[SortVar, Sort]): OpDecl = {
    OpDecl(name, typ.substituteSort(binding))
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
    * Substitute sort variables in this constructor.
    *
    * @param binding
    * @return
    */
  override def substituteSort(binding: Map[SortVar, Sort]): Signature = {
    OpDeclInj(typ.substituteSort(binding))
  }

  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    s"$typ"
}

abstract class Type {
  def substituteSort(binding: Map[SortVar, Sort]): Type
}

case class FunType(children: List[Type], result: Type) extends Type {
  /**
    * Substitute sort in this type.
    *
    * @param binding
    * @return
    */
  override def substituteSort(binding: Map[SortVar, Sort]): Type = {
    FunType(children.map(_.substituteSort(binding)), result.substituteSort(binding))
  }

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
    * Substitute sort in this type.
    *
    * @param binding
    * @return
    */
  override def substituteSort(binding: Map[SortVar, Sort]): Type = {
    ConstType(sort.substituteSort(binding))
  }

  /**
    * The Stratego representation of a signature.
    *
    * @return
    */
  override def toString: String =
    sort.toString
}
