package nl.tudelft.fragments

//abstract class Term[T] {
//  def substitute(binding: TermBinding): Term[T]
//
//  def unify(t: Term[T], binding: TermBinding = Map.empty): Option[TermBinding]
//
//  def freshen(nameBinding: Map[String, String]): (Map[String, String], T)
//}
//
//case class TermAppl[T](name: String, children: List[Term[T]] = Nil) extends Term[T] {
//  override def substitute(binding: TermBinding): Term[T] =
//    TermAppl(name, children.map(_.substitute(binding)))
//
//  override def unify(typ: Term[T], binding: TermBinding): Option[TermBinding] = typ match {
//    case c@TermAppl(`name`, _) if children.length == c.children.length =>
//      children.zip(c.children).foldLeftWhile(binding) {
//        case (binding, (t1, t2)) =>
//          t1.unify(t2, binding)
//      }
//    case TermVar(_) =>
//      typ.unify(this, binding)
//    case _ =>
//      None
//  }
//
//  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Term) =
//    children.freshen(nameBinding).map { case (nameBinding, children) =>
//      (nameBinding, TermAppl[T](name, children))
//    }
//
//  override def toString: String =
//    s"""TermAppl("$name", $children)"""
//}
//
//case class TermVar[T](name: String) extends Term[T] {
//  override def substitute(binding: TermBinding): Term[T] =
//    binding.getOrElse(this, this)
//
//  override def unify(term: Term[T], binding: TermBinding): Option[TermBinding] = term match {
//    case t@TermVar(_) if binding.contains(t) =>
//      unify(binding(t), binding)
//    case _ =>
//      if (binding.contains(this)) {
//        binding(this).unify(term, binding)
//      } else {
//        Some(binding + (this -> term))
//      }
//  }
//
//  override def freshen(nameBinding: Map[String, String]): (Map[String, String], T) = {
//    if (nameBinding.contains(name)) {
//      (nameBinding, TermVar(nameBinding(name)))
//    } else {
//      val fresh = "t" + NameProvider.next
//      (nameBinding + (name -> fresh), TermVar(fresh))
//    }
//  }
//
//  override def toString: String =
//    s"""TermVar("$name")"""
//}
