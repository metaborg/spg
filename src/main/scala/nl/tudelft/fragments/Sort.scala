package nl.tudelft.fragments

abstract class Sort {
  def substituteSort(binding: SortBinding): Sort

  def unify(s: Sort, binding: SortBinding = Map.empty): Option[SortBinding]
}

case class SortAppl(cons: String, children: List[Sort] = Nil) extends Sort {
  override def substituteSort(binding: SortBinding): Sort =
    SortAppl(cons, children.map(_.substituteSort(binding)))

  override def unify(sort: Sort, binding: SortBinding): Option[SortBinding] = sort match {
    case c@SortAppl(`cons`, _) if children.length == c.children.length =>
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
    s"""SortAppl("$cons", $children)"""
}

case class SortVar(name: String) extends Sort {
  override def substituteSort(binding: SortBinding): Sort =
    binding.getOrElse(this, this)

  override def unify(sort: Sort, binding: SortBinding): Option[SortBinding] = sort match {
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
