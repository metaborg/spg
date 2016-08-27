package nl.tudelft.fragments.spoofax.models

import nl.tudelft.fragments._

abstract class Symbol

// Sort
abstract class Sort extends Symbol {
  def substituteSort(binding: Map[SortVar, Sort]): Sort

  def unify(sort: Sort, binding: Map[SortVar, Sort] = Map.empty): Option[Map[SortVar, Sort]]
}

object Sort {
  def injections(signatures: List[Signature])(sort: Sort): Set[Sort] = {
    val sorts = signatures.flatMap {
      case OpDeclInj(FunType(List(ConstType(x)), ConstType(y))) =>
        y.unify(sort).map(x.substituteSort)
      case _ =>
        Option.empty[Sort]
    }

    sorts.toSet
  }

  def transitiveInjections(signatures: List[Signature])(sorts: Set[Sort]): Set[Sort] =
    sorts.flatMap(injections(signatures)) ++ sorts

  def injectionsClosure(signatures: List[Signature])(sorts: Set[Sort]): Set[Sort] =
    fixedPoint(transitiveInjections(signatures), sorts)
}

case class SortAppl(name: String, children: List[Sort] = Nil) extends Sort {
  override def substituteSort(binding: Map[SortVar, Sort]): Sort =
    SortAppl(name, children.map(_.substituteSort(binding)))

  override def unify(sort: Sort, binding: Map[SortVar, Sort]): Option[Map[SortVar, Sort]] = sort match {
    case c@SortAppl(`name`, _) if children.length == c.children.length =>
      children.zip(c.children).foldLeftWhile(binding) {
        case (binding, (t1, t2)) =>
          t1.unify(t2, binding)
      }
    case SortVar(_) =>
      sort.unify(this, binding)
    case _ =>
      None
  }

  override def toString: String =
    s"""SortAppl("$name", $children)"""
}

case class SortVar(name: String) extends Sort {
  override def substituteSort(binding: Map[SortVar, Sort]): Sort =
    binding.getOrElse(this, this)

  override def unify(sort: Sort, binding: Map[SortVar, Sort]): Option[Map[SortVar, Sort]] = sort match {
    case s@SortVar(_) if binding.contains(s) =>
      unify(binding(s), binding)
    case _ =>
      if (binding.contains(this)) {
        binding(this).unify(sort, binding)
      } else {
        Some(binding + (this -> sort))
      }
  }

  override def toString: String =
    s"""SortVar("$name")"""
}

// Literal string
case class Lit(text: String) extends Symbol

// Optional symbol
case class Opt(symbol: Symbol) extends Symbol

// Alternative symbol
case class Alt(s1: Symbol, s2: Symbol) extends Symbol

// Kleene plus
case class Iter(symbol: Symbol) extends Symbol

// Kleene star
case class IterStar(symbol: Symbol) extends Symbol

// Kleene plus with separator
case class IterSep(symbol: Symbol, separator: String) extends Symbol

// Kleene star with separator
case class IterStarSep(symbol: Symbol, separator: String) extends Symbol
