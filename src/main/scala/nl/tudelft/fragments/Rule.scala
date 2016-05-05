package nl.tudelft.fragments

// Globals
object NameProvider {
  // Start high enough to prevent clashes with names in the original rules
  var c = 9

  def next = {
    c = c + 1
    c
  }
}

// Rule
case class Rule(pattern: Pattern, typ: Type, constraints: List[Constraint]) {
  def merge(hole: TermVar, rule: Rule): Rule = {
    val freshRule = rule.freshen()

    val unifier = hole.typ.unify(freshRule.typ).get

    val merged = Rule(
      pattern =
        pattern.substituteTerm(Map(hole -> freshRule.pattern)),
      typ =
        typ,
      constraints =
        freshRule.constraints ++ constraints
    )

    merged.substituteType(unifier)
  }

  def substituteType(binding: TypeBinding): Rule =
    Rule(pattern.substituteType(binding), typ.substituteType(binding), constraints.substituteType(binding))

  def freshen(nameBinding: NameBinding = Map.empty): Rule = {
    typ.freshen(nameBinding).map { case (nameBinding, typ) =>
      pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
        constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
          Rule(pattern, typ, constraints)
        }
      }
    }
  }
}

// Constraint
abstract class Constraint {
  def substituteType(binding: TypeBinding): Constraint

  def freshen(nameBinding: NameBinding): (NameBinding, Constraint)
}

case class TypeOf(n: String, t: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeOf(n, t.substituteType(binding))

  override def freshen(nameBinding: NameBinding): (NameBinding, Constraint) =
    t.freshen(nameBinding).map { case (nameBinding, t) =>
      (nameBinding, TypeOf(nameBinding.getOrElse(n, n), t))
    }
}

case class TypeEquals(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeEquals(t1.substituteType(binding), t2.substituteType(binding))

  override def freshen(nameBinding: NameBinding): (NameBinding, Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, TypeEquals(t1, t2))
      }
    }
}

case class Subtype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    Subtype(t1.substituteType(binding), t2.substituteType(binding))

  override def freshen(nameBinding: NameBinding): (NameBinding, Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, Subtype(t1, t2))
      }
    }
}

// Pattern
abstract class Pattern {
  def vars: List[TermVar]

  def substituteTerm(binding: Map[TermVar, Pattern]): Pattern

  def substituteType(binding: TypeBinding): Pattern

  def freshen(nameBinding: NameBinding): (NameBinding, Pattern)
}

case class TermAppl(cons: String, children: List[Pattern] = List.empty) extends Pattern {
  override def vars: List[TermVar] =
    children.flatMap(_.vars).distinct

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substituteTerm(binding)))

  override def substituteType(typeBinding: TypeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteType(typeBinding)))

  override def freshen(nameBinding: NameBinding): (NameBinding, Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }
}

case class TermVar(name: String, typ: Type) extends Pattern {
  override def vars: List[TermVar] =
    List(this)

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    binding.getOrElse(this, this)

  override def substituteType(typeBinding: TypeBinding): Pattern =
    TermVar(name, typ.substituteType(typeBinding))

  override def freshen(nameBinding: NameBinding): (NameBinding, Pattern) =
    typ.freshen(nameBinding).map { case (nameBinding, typ) =>
      (nameBinding, TermVar(nameBinding.getOrElse(name, name), typ))
    }
}

case class NameVar(name: String) extends Pattern {
  override def vars: List[TermVar] =
    List.empty

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    this

  override def substituteType(typeBinding: TypeBinding): Pattern =
    this

  override def freshen(nameBinding: NameBinding): (NameBinding, Pattern) =
    ???
}

// Scope
abstract class Scope

case class ScopeVar(name: String) extends Scope

// Type
abstract class Type {
  def substituteType(binding: TypeBinding): Type

  def unify(t: Type, binding: TypeBinding = Map.empty): Option[TypeBinding]

  def freshen(nameBinding: NameBinding): (NameBinding, Type)
}

case class TypeAppl(name: String, children: List[Type] = List.empty) extends Type {
  override def substituteType(binding: TypeBinding): Type =
    TypeAppl(name, children.map(_.substituteType(binding)))

  override def unify(typ: Type, binding: TypeBinding): Option[TypeBinding] = typ match {
    case c@TypeAppl(`name`, _) if children.length == c.children.length =>
      children.zip(c.children).foldLeftWhile(binding) {
        case (binding, (t1, t2)) =>
          t1.unify(t2, binding)
      }
    case TypeVar(_) =>
      typ.unify(this, binding)
    case _ =>
      None
  }

  override def freshen(nameBinding: NameBinding): (NameBinding, Type) =
    children.freshen(nameBinding).map { case (nameBinding, children) =>
      (nameBinding, TypeAppl(name, children))
    }
}

case class TypeVar(name: String) extends Type {
  override def substituteType(binding: TypeBinding): Type =
    binding.getOrElse(this, this)

  override def unify(typ: Type, typeBinding: TypeBinding): Option[TypeBinding] = typ match {
    case t@TypeVar(_) if typeBinding.contains(t) =>
      unify(typeBinding(t), typeBinding)
    case _ =>
      if (typeBinding.contains(this)) {
        typeBinding(this).unify(typ, typeBinding)
      } else {
        Some(typeBinding + (this -> typ))
      }
  }

  override def freshen(nameBinding: NameBinding): (NameBinding, Type) = {
    if (nameBinding.contains(name)) {
      (nameBinding, TypeVar(nameBinding(name)))
    } else {
      val fresh = "t" + NameProvider.next
      (nameBinding + (name -> fresh), TypeVar(fresh))
    }
  }
}
