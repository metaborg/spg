package org.metaborg.spg.core.sdf

// TODO: Sorts are just patterns. No need to duplicate all the code...

//abstract class Sort extends Pattern

//class SortAppl(cons: String, children: List[Sort] = Nil) extends TermAppl(cons, children)

//class SortVar(name: String) extends Var(name)

//---

abstract class Sort extends Symbol {
  def substituteSort(binding: Map[SortVar, Sort]): Sort

  def unify(sort: Sort, binding: Map[SortVar, Sort] = Map.empty): Option[Map[SortVar, Sort]]
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
