package org.metaborg.spg.core

abstract class Pattern {
  def vars: List[Var]

  def size: Int

  def names: List[SymbolicName]

  def substitute(binding: Map[Var, Pattern]): Pattern

  def substituteScope(binding: TermBinding): Pattern

  def substituteType(binding: TermBinding): Pattern =
    substituteScope(binding)

  def substituteSort(binding: SortBinding): Pattern

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern)

  def unify(t: Pattern, termBinding: TermBinding = Map.empty): Option[TermBinding]

  def contains(p: Pattern): Boolean

  def find(f: Pattern => Boolean): Option[Pattern]

  /**
    * Apply f at every node in the tree and collect all results f(x).
    *
    * @param f
    * @return
    */
  def collect(f: Pattern => List[Pattern]): List[Pattern]
}

case class Var(name: String) extends Pattern {
  override def vars: List[Var] =
    List(this)

  override def size: Int =
    1

  override def names: List[SymbolicName] =
    Nil

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    binding.getOrElse(this, this)

  override def substituteScope(binding: TermBinding): Pattern =
    binding.getOrElse(this, this)

  override def substituteSort(binding: SortBinding): Pattern =
    Var(name)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name)) {
      (nameBinding, Var(nameBinding(name)))
    } else {
      val fresh = "x" + nameProvider.next
      (nameBinding + (name -> fresh), Var(fresh))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = {
    if (this == typ) {
      Some(termBinding)
    } else if (typ.contains(this)) {
      None
    } else {
      typ match {
        case t@Var(_) if termBinding.contains(t) =>
          unify(termBinding(t), termBinding)
        case _ =>
          if (termBinding.contains(this)) {
            termBinding(this).unify(typ, termBinding)
          } else {
            Some(termBinding + (this -> typ))
          }
      }
    }
  }

  override def contains(p: Pattern): Boolean =
    false

  override def toString: String =
    s"""Var("$name")"""

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    if (f(this)) {
      Some(this)
    } else {
      None
    }

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    f(this)
}

abstract class Term extends Pattern

case class As(alias: Var, term: Pattern) extends Term {
  override def vars: List[Var] =
    term.vars

  override def size: Int =
    term.size

  override def names: List[SymbolicName] =
    term.names

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    if (binding.contains(alias)) {
      binding(alias)
    } else {
      As(alias, term.substitute(binding))
    }

  override def substituteScope(binding: TermBinding): Pattern =
    As(alias, term.substituteScope(binding))

  override def substituteSort(binding: SortBinding): Pattern =
    As(alias, term.substituteSort(binding))

  override def freshen(binding: Map[String, String]): (Map[String, String], Pattern) =
    alias.freshen(binding).map { case (binding, alias) =>
      term.freshen(binding).map { case (binding, term) =>
        (binding, As(alias.asInstanceOf[Var], term))
      }
    }

  override def unify(t: Pattern, termBinding: TermBinding): Option[TermBinding] = t match {
    case o@As(_, _) =>
      term.unify(o.term, termBinding).flatMap(termBinding =>
        o.alias.unify(o.alias, termBinding)
      )
    case TermAppl(_, _) =>
      t.unify(term, termBinding)
    case Var(_) =>
      t.unify(this, termBinding)
    case _ =>
      None
  }

  override def contains(p: Pattern): Boolean =
    term.contains(p)

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    ???

  override def collect(f: (Pattern) => List[Pattern]): List[Pattern] =
    f(this) ++ term.collect(f)
}

/**
  * A constructor application
  *
  * @param cons
  * @param children
  */
case class TermAppl(cons: String, children: List[Pattern] = Nil) extends Term {
  override def vars: List[Var] =
    children.flatMap(_.vars).distinct

  override def size: Int =
    1 + children.map(_.size).sum

  override def names: List[SymbolicName] =
    children.flatMap(_.names).distinct

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substitute(binding)))

  override def substituteScope(binding: TermBinding): Pattern =
    TermAppl(cons, children.map(_.substituteScope(binding)))

  override def substituteSort(binding: SortBinding): Pattern =
    TermAppl(cons, children.map(_.substituteSort(binding)))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }

  override def unify(typ: Pattern, termBinding: TermBinding): Option[TermBinding] = {
    typ match {
      case c@TermAppl(`cons`, _) if children.length == c.children.length =>
        children.zip(c.children).foldLeftWhile(termBinding) {
          case (termBinding, (t1, t2)) =>
            t1.unify(t2, termBinding)
        }
      case Var(_) =>
        typ.unify(this, termBinding)
      case _ =>
        None
    }
  }

  override def contains(p: Pattern): Boolean =
    children.exists(child => child == p || child.contains(p))

  override def toString: String =
    s"""TermAppl("$cons", $children)"""

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    if (f(this)) {
      Some(this)
    } else {
      for (child <- children) {
        child.find(f) match {
          case x@Some(_) => return x
          case _ =>
        }
      }

      None
    }

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    f(this) ++ children.flatMap(_.collect(f))

  def arity: Int =
    children.length
}

case class TermString(name: String) extends Term {
  override def vars: List[Var] =
    Nil

  override def names: List[SymbolicName] =
    Nil

  override def size: Int =
    1

  override def unify(t: Pattern, termBinding: TermBinding): Option[TermBinding] =
    ???

  override def contains(p: Pattern): Boolean =
    ???

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    this

  override def substituteScope(binding: TermBinding): Pattern =
    ???

  override def substituteSort(binding: SortBinding): Pattern =
    ???

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    ???

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    None

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    Nil

  override def toString =
    s"""TermString("$name")"""
}

abstract class Name extends Pattern {
  def namespace: String

  def name: String
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
    case ConcreteName(`namespace`, name, _) =>
      ???
    case v@Var(_) =>
      v.unify(this, termBinding)
    case _ =>
      None
  }

  override def contains(p: Pattern): Boolean =
    false

  override def vars: List[Var] =
    Nil

  override def toString: String =
    s"""SymbolicName("$namespace", "$name")"""

  override def size: Int =
    1

  override def names: List[SymbolicName] =
    ???

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    this

  override def substituteScope(binding: TermBinding): Pattern =
    this

  override def substituteSort(binding: SortBinding): Pattern =
    this

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    None

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    Nil
}

case class ConcreteName(namespace: String, name: String, position: Int) extends Name {
  override def vars: List[Var] =
    Nil

  override def names: List[SymbolicName] =
    ???

  override def size: Int =
    ???

  override def unify(t: Pattern, termBinding: TermBinding): Option[TermBinding] = t match {
    case ConcreteName(`namespace`, `name`, _) =>
      Some(termBinding)
    case s@SymbolicName(`namespace`, name) =>
      Some(termBinding)
    case Var(_) =>
      t.unify(this, termBinding)
    case _ =>
      None
  }

  override def contains(p: Pattern): Boolean =
    false

  override def substitute(binding: Map[Var, Pattern]): Pattern =
    this

  override def substituteSort(binding: SortBinding): Pattern =
    ???

  override def substituteScope(binding: TermBinding): Pattern =
    ???

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name + "@" + position)) {
      (nameBinding, ConcreteName(namespace, name, nameBinding(name + "@" + position).toInt))
    } else {
      val fresh = nameProvider.next
      (nameBinding + (name + "@" + position -> fresh.toString), ConcreteName(namespace, name, fresh))
    }

  override def find(f: (Pattern) => Boolean): Option[Pattern] =
    None

  override def collect(f: Pattern => List[Pattern]): List[Pattern] =
    Nil

  override def toString: String =
    s"""ConcreteName("$namespace", "$name", $position)"""
}
