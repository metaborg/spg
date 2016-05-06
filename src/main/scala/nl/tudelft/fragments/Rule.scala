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
case class Rule(pattern: Pattern, sort: String, typ: Type, scope: Scope, constraints: List[Constraint]) {
  def merge(hole: TermVar, rule: Rule): Rule = {
    val freshRule = rule.freshen()

    val typeUnifier = hole.typ.unify(freshRule.typ).get
    val scopeUnifier = hole.scope.unify(freshRule.scope).get

    val merged = Rule(
      pattern =
        pattern.substituteTerm(Map(hole -> freshRule.pattern)),
      sort =
        sort,
      typ =
        typ,
      scope =
        scope,
      constraints =
        freshRule.constraints ++ constraints
    )

    merged
      .substituteType(typeUnifier)
      .substituteScope(scopeUnifier)
  }

  def substituteType(binding: TypeBinding): Rule =
    Rule(pattern.substituteType(binding), sort, typ.substituteType(binding), scope, constraints.substituteType(binding))

  def substituteScope(binding: ScopeBinding): Rule =
    Rule(pattern.substituteScope(binding), sort, typ, scope.substituteScope(binding), constraints.substituteScope(binding))

  def freshen(nameBinding: Map[String, String] = Map.empty): Rule = {
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      typ.freshen(nameBinding).map { case (nameBinding, typ) =>
        scope.freshen(nameBinding).map { case (nameBinding, scope) =>
          constraints.freshen(nameBinding).map { case (nameBinding, constraints) =>
            Rule(pattern, sort, typ, scope, constraints)
          }
        }
      }
    }
  }

  override def toString: String =
    s"""Rule($pattern, "$sort", $typ, $scope, $constraints)"""
}

// Constraint
abstract class Constraint {
  def substituteType(binding: TypeBinding): Constraint

  def substituteScope(binding: ScopeBinding): Constraint

  def substituteName(binding: NameBinding): Constraint

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint)
}

// Facts
case class Par(s1: Scope, s2: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Par(s1.substituteScope(binding), s2.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, Par(s1, s2))
      }
    }
}

case class Dec(s: Scope, n: NameVar) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Dec(s.substituteScope(binding), n)

  override def substituteName(binding: NameBinding): Constraint =
    Dec(s, n.substituteName(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, Dec(s, n))
      }
    }
}

case class Ref(n: NameVar, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Ref(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    Ref(n.substituteName(binding), s)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, Ref(n, s))
      }
    }
}

// Proper constraints
case class Res(n1: NameVar, n2: NameVar) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Res(n1, n2)

  override def substituteName(binding: NameBinding): Constraint =
    Res(n1.substituteName(binding), n2.substituteName(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Res(n1, n2))
      }
    }
}

case class TypeOf(n: NameVar, t: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeOf(n, t.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    TypeOf(n.substituteName(binding), t)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t.freshen(nameBinding).map { case (nameBinding, t) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, TypeOf(n, t))
      }
    }
}

case class TypeEquals(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeEquals(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, TypeEquals(t1, t2))
      }
    }
}

case class Subtype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    Subtype(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, Subtype(t1, t2))
      }
    }
}

// Pattern
abstract class Pattern {
  def vars: List[TermVar]

  def size: Int

  def substituteTerm(binding: Map[TermVar, Pattern]): Pattern

  def substituteType(binding: TypeBinding): Pattern

  def substituteScope(binding: ScopeBinding): Pattern

  def substituteName(binding: NameBinding): Pattern

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern)
}

case class TermAppl(cons: String, children: List[Pattern] = List.empty) extends Pattern {
  override def vars: List[TermVar] =
    children.flatMap(_.vars).distinct

  override def size: Int =
    1 + children.map(_.size).sum

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substituteTerm(binding)))

  override def substituteType(binding: TypeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteType(binding)))

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteScope(binding)))

  override def substituteName(binding: NameBinding): Pattern =
    TermAppl(cons, children.map(_.substituteName(binding)))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }

  override def toString: String =
    s"""TermAppl("$cons", $children)"""
}

case class TermVar(name: String, sort: String, typ: Type, scope: Scope) extends Pattern {
  override def vars: List[TermVar] =
    List(this)

  override def size: Int =
    0

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    binding.getOrElse(this, this)

  override def substituteType(binding: TypeBinding): Pattern =
    TermVar(name, sort, typ.substituteType(binding), scope)

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermVar(name, sort, typ, scope.substituteScope(binding))

  override def substituteName(binding: NameBinding): Pattern =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    typ.freshen(nameBinding).map { case (nameBinding, typ) =>
      scope.freshen(nameBinding).map { case (nameBinding, scope) =>
        if (nameBinding.contains(name)) {
          (nameBinding, TermVar(nameBinding(name), sort, typ, scope))
        } else {
          val fresh = "x" + NameProvider.next
          (nameBinding + (name -> fresh), TermVar(fresh, sort, typ, scope))
        }
      }
    }

  override def toString: String =
    s"""TermVar("$name", "$sort", $typ, $scope)"""
}

case class NameVar(name: String) extends Pattern {
  override def vars: List[TermVar] =
    List.empty

  override def size: Int =
    1

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    this

  override def substituteType(typeBinding: TypeBinding): Pattern =
    this

  override def substituteScope(binding: ScopeBinding): Pattern =
    this

  override def substituteName(binding: NameBinding): NameVar =
    binding.getOrElse(this, this)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], NameVar) =
    if (nameBinding.contains(name)) {
      (nameBinding, NameVar(nameBinding(name)))
    } else {
      val fresh = "n" + NameProvider.next
      (nameBinding + (name -> fresh), NameVar(fresh))
    }

  override def toString: String =
    s"""NameVar("$name")"""
}

// Scope
abstract class Scope {
  def unify(t: Scope, binding: ScopeBinding = Map.empty): Option[ScopeBinding]

  def substituteScope(binding: ScopeBinding): Scope

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Scope)
}

case class ScopeVar(name: String) extends Scope {
  override def unify(scope: Scope, binding: ScopeBinding): Option[ScopeBinding] = scope match {
    case s@ScopeVar(_) if binding.contains(s) =>
      unify(binding(s), binding)
    case _ =>
      if (binding.contains(this)) {
        binding(this).unify(scope, binding)
      } else {
        Some(binding + (this -> scope))
      }
  }

  override def substituteScope(binding: ScopeBinding): Scope =
    binding.getOrElse(this, this)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Scope) =
    if (nameBinding.contains(name)) {
      (nameBinding, ScopeVar(nameBinding(name)))
    } else {
      val fresh = "s" + NameProvider.next
      (nameBinding + (name -> fresh), ScopeVar(fresh))
    }

  override def toString: String =
    s"""ScopeVar("$name")"""
}

// Type
abstract class Type {
  def substituteType(binding: TypeBinding): Type

  def unify(t: Type, binding: TypeBinding = Map.empty): Option[TypeBinding]

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Type)
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

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Type) =
    children.freshen(nameBinding).map { case (nameBinding, children) =>
      (nameBinding, TypeAppl(name, children))
    }

  override def toString: String =
    s"""TypeAppl("$name", $children)"""
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

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Type) = {
    if (nameBinding.contains(name)) {
      (nameBinding, TypeVar(nameBinding(name)))
    } else {
      val fresh = "t" + NameProvider.next
      (nameBinding + (name -> fresh), TypeVar(fresh))
    }
  }

  override def toString: String =
    s"""TypeVar("$name")"""
}
