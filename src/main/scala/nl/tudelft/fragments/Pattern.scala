package nl.tudelft.fragments

abstract class Pattern {
  def vars: List[TermVar]

  def size: Int

  def names: List[SymbolicName]

  def substitute(binding: Map[TermVar, Pattern]): Pattern

  def substituteScope(binding: ScopeBinding): Pattern

  def substituteSort(binding: SortBinding): Pattern

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern)

  def unify(t: Pattern, termBinding: TermBinding = Map.empty): Option[TermBinding]
}

case class TermAppl(cons: String, children: List[Pattern] = Nil) extends Pattern {
  override def vars: List[TermVar] =
    children.flatMap(_.vars).distinct

  override def size: Int =
    1 + children.map(_.size).sum

  override def names: List[SymbolicName] =
    children.flatMap(_.names).distinct

  override def substitute(binding: Map[TermVar, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substitute(binding)))

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteScope(binding)))

  override def substituteSort(binding: SortBinding): Pattern =
    TermAppl(cons, children.map(_.substituteSort(binding)))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = typ match {
    case c@TermAppl(`cons`, _) if children.length == c.children.length =>
      children.zip(c.children).foldLeftWhile(termBinding) {
        case (termBinding, (t1, t2)) =>
          t1.unify(t2, termBinding)
      }
    case TermVar(_) =>
      typ.unify(this, termBinding)
    case _ =>
      None
  }

  override def toString: String =
    s"""TermAppl("$cons", $children)"""
}

case class TermVar(name: String) extends Pattern {
  override def vars: List[TermVar] =
    List(this)

  override def size: Int =
    0

  override def names: List[SymbolicName] =
    Nil

  override def substitute(binding: Map[TermVar, Pattern]): Pattern =
    binding.getOrElse(this, this)

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermVar(name)

  override def substituteSort(binding: SortBinding): Pattern =
    TermVar(name)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name)) {
      (nameBinding, TermVar(nameBinding(name)))
    } else {
      val fresh = "x" + nameProvider.next
      (nameBinding + (name -> fresh), TermVar(fresh))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = {
    typ match {
      case t@TermVar(_) if termBinding.contains(t) =>
        unify(termBinding(t), termBinding)
      case _ =>
        if (termBinding.contains(this)) {
          termBinding(this).unify(typ, termBinding)
        } else {
          Some(termBinding + (this -> typ))
        }
    }
  }

  override def toString: String =
    s"""TermVar("$name")"""
}

abstract class Name extends Pattern {
  def namespace: String
}

case class SymbolicName(namespace: String, name: String) extends Name {
  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name)) {
      (nameBinding, SymbolicName(namespace, nameBinding(name)))
    } else {
      val fresh = "n" + nameProvider.next
      (nameBinding + (name -> fresh), SymbolicName(namespace, fresh))
    }

  override def unify(t: Pattern, termBinding: TermBinding = Map.empty): Option[TermBinding] = t match {
    case SymbolicName(`namespace`, `name`) =>
      Some(Map())
    case v@TermVar(_) =>
      v.unify(this, termBinding)
    case _ =>
      None
  }

  override def vars: List[TermVar] =
    Nil

  override def toString: String =
    s"""SymbolicName("$namespace", "$name")"""


  override def size: Int = ???

  override def names: List[SymbolicName] = ???

  override def substitute(binding: Map[TermVar, Pattern]): Pattern =
    this

  override def substituteScope(binding: ScopeBinding): Pattern =
    this

  override def substituteSort(binding: SortBinding): Pattern =
    this
}

case class ConcreteName(namespace: String, name: String) extends Name {
  override def vars: List[TermVar] =
    ???

  override def names: List[SymbolicName] =
    ???

  override def size: Int =
    ???

  override def unify(t: Pattern, termBinding: TermBinding): Option[TermBinding] =
    ???

  override def substituteSort(binding: SortBinding): Pattern =
    ???

  override def substitute(binding: Map[TermVar, Pattern]): Pattern =
    ???

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    ???

  override def substituteScope(binding: ScopeBinding): Pattern =
    ???
}
