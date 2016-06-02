package nl.tudelft.fragments

// Rule
case class Rule(sort: Sort, typ: Type, scopes: List[Scope], state: State) {
  def mergex(hole: TermVar, rule: Rule): (Rule, Map[String, String]) = {
    val (nameBinding, freshRule) = rule.freshen()

    val typeUnifier = hole.typ.unify(freshRule.typ).get
    val scopeUnifier = hole.scope.unify(freshRule.scopes).get
    val sortUnifier = hole.sort.unify(freshRule.sort).get

    val merged = copy(
      state =
        state.merge(hole, freshRule.state)
    )

    // TODO: 1) The merge may allow us to solve constraints. E.g. merging something with type 't' with something with type 'Int' gives t = Int.
    // TODO: Now we only substitute the type unifier, but we may propagate this info by solving as well?

    // TODO: 2) Unify `mergex` and `merge` methods.. we don't want duplicate code!

    (merged
      .substituteType(typeUnifier._1)
      .substituteName(typeUnifier._2)
      .substituteScope(scopeUnifier)
      .substituteSort(sortUnifier)
    , nameBinding)
  }

  def merge(hole: TermVar, rule: Rule): Rule = {
    val (_, freshRule) = rule.freshen()

    val typeUnifier = hole.typ.unify(freshRule.typ).get
    val scopeUnifier = hole.scope.unify(freshRule.scopes).get
    val sortUnifier = hole.sort.unify(freshRule.sort).get

    val merged = copy(
      state =
        state.merge(hole, freshRule.state)
    )

    merged
      .substituteType(typeUnifier._1)
      .substituteName(typeUnifier._2)
      .substituteScope(scopeUnifier)
      .substituteSort(sortUnifier)
  }

  def points: List[(Pattern, Sort, List[Scope])] =
    if (sort != SortAppl("ProgramDecl")) {
      (pattern, sort, scopes) :: pattern.points
    } else {
      pattern.points
    }

  // Backwards compatibility
  def constraints: List[Constraint] =
    state.constraints

  // Backwards compatibility
  def pattern: Pattern =
    state.pattern

  def substituteType(binding: TypeBinding): Rule =
    Rule(sort, typ.substituteType(binding), scopes, state.substituteType(binding))

  def substituteName(binding: NameBinding): Rule =
    Rule(sort, typ.substituteName(binding), scopes, state.substituteName(binding))

  def substituteScope(binding: ScopeBinding): Rule =
    Rule(sort, typ, scopes.substituteScope(binding), state.substituteScope(binding))

  def substituteSort(binding: SortBinding): Rule =
    Rule(sort.substituteSort(binding), typ, scopes, state)

  def freshen(nameBinding: Map[String, String] = Map.empty): (Map[String, String], Rule) = {
    typ.freshen(nameBinding).map { case (nameBinding, typ) =>
      scopes.freshen(nameBinding).map { case (nameBinding, scope) =>
        state.freshen(nameBinding).map { case (nameBinding, state) =>
          (nameBinding, Rule(sort, typ, scope, state))
        }
      }
    }
  }

  override def toString: String =
    s"""Rule($sort, $typ, $scopes, $state)"""
}

// Backwards compatibility
object Rule {
  def apply(pattern: Pattern, sort: Sort, typ: Type, scopes: List[Scope], state: State): Rule =
    Rule(sort, typ, scopes, state.copy(pattern = pattern))
}

// Constraint
abstract class Constraint {
  def substituteType(binding: TypeBinding): Constraint

  def substituteScope(binding: ScopeBinding): Constraint

  def substituteName(binding: NameBinding): Constraint

  def substituteConcrete(binding: ConcreteBinding): Constraint

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint)

  def isProper: Boolean = false
}

// Facts
case class Par(s1: Scope, s2: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Par(s1.substituteScope(binding), s2.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, Par(s1, s2))
      }
    }
}

case class Dec(s: Scope, n: Name) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Dec(s.substituteScope(binding), n)

  override def substituteName(binding: NameBinding): Constraint =
    Dec(s, n.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Dec(s, n.substituteConcrete(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, Dec(s, n))
      }
    }
}

case class Ref(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Ref(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    Ref(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Ref(n.substituteConcrete(binding), s)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, Ref(n, s))
      }
    }
}

case class DirectImport(s1: Scope, s2: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    DirectImport(s1.substituteScope(binding), s2.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, DirectImport(s1, s2))
      }
    }
}

case class AssociatedImport(s: Scope, n: Name) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    AssociatedImport(s, n.substituteName(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    AssociatedImport(s.substituteScope(binding), n)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    AssociatedImport(s, n.substituteConcrete(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, AssociatedImport(s, n))
      }
    }
}

case class AssocFact(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    AssocFact(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    AssocFact(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    AssocFact(n.substituteConcrete(binding), s)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, AssocFact(n, s))
      }
    }
}

// Proper constraints
case class AssocConstraint(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    AssocConstraint(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    AssocConstraint(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    AssocConstraint(n.substituteConcrete(binding), s)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, AssocConstraint(n, s))
      }
    }

  override def isProper =
    true
}

case class Res(n1: Name, n2: Name) extends Constraint {
  def substitute(nameBinding: Map[String, String]) =
    Res(n1.substitute(nameBinding), n2.substitute(nameBinding))

  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Res(n1, n2)

  override def substituteName(binding: NameBinding): Constraint =
    Res(n1.substituteName(binding), n2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Res(n1.substituteConcrete(binding), n2.substituteConcrete(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Res) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Res(n1, n2))
      }
    }

  override def isProper =
    true
}

case class TypeOf(n: Name, t: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeOf(n, t.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    TypeOf(n.substituteName(binding), t.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    TypeOf(n.substituteConcrete(binding), t)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t.freshen(nameBinding).map { case (nameBinding, t) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, TypeOf(n, t))
      }
    }

  override def isProper =
    true
}

case class TypeEquals(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    TypeEquals(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    TypeEquals(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    TypeEquals(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, TypeEquals(t1, t2))
      }
    }

  override def isProper =
    true
}

case class Subtype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    Subtype(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    Subtype(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Subtype(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, Subtype(t1, t2))
      }
    }

  override def isProper =
    true
}

// Sort
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

// Pattern
abstract class Pattern {
  def vars: List[TermVar]

  def points: List[(Pattern, Sort, List[Scope])]

  // Number of terms in the pattern, TermVars excluded
  def size: Int

  def names: List[SymbolicName]

  def substituteTerm(binding: Map[TermVar, Pattern]): Pattern

  def substituteType(binding: TypeBinding): Pattern

  def substituteScope(binding: ScopeBinding): Pattern

  def substituteName(binding: NameBinding): Pattern

  def substituteConcrete(binding: ConcreteBinding): Pattern

  def substituteSort(binding: SortBinding): Pattern

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern)
}

case class TermAppl(cons: String, children: List[Pattern] = Nil) extends Pattern {
  override def vars: List[TermVar] =
    children.flatMap(_.vars).distinct

  override def points: List[(Pattern, Sort, List[Scope])] =
    vars.map(hole => (hole, hole.sort, hole.scope))

  override def size: Int =
    1 + children.map(_.size).sum

  override def names: List[SymbolicName] =
    children.flatMap(_.names).distinct

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    TermAppl(cons, children.map(_.substituteTerm(binding)))

  override def substituteType(binding: TypeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteType(binding)))

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermAppl(cons, children.map(_.substituteScope(binding)))

  override def substituteName(binding: NameBinding): Pattern =
    TermAppl(cons, children.map(_.substituteName(binding)))

  override def substituteConcrete(binding: ConcreteBinding): Pattern =
    TermAppl(cons, children.map(_.substituteConcrete(binding)))

  override def substituteSort(binding: SortBinding): Pattern =
    TermAppl(cons, children.map(_.substituteSort(binding)))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    children.freshen(nameBinding).map { case (nameBinding, args) =>
      (nameBinding, TermAppl(cons, args))
    }

  override def toString: String =
    s"""TermAppl("$cons", $children)"""
}

case class TermVar(name: String, sort: Sort, typ: Type, scope: List[Scope]) extends Pattern {
  override def vars: List[TermVar] =
    List(this)

  override def points =
    ???

  override def size: Int =
    0

  override def names: List[SymbolicName] =
    Nil

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    binding.getOrElse(this, this)

  override def substituteType(binding: TypeBinding): Pattern =
    TermVar(name, sort, typ.substituteType(binding), scope)

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermVar(name, sort, typ, scope.substituteScope(binding))

  override def substituteName(binding: NameBinding): Pattern =
    this

  override def substituteConcrete(binding: ConcreteBinding): Pattern =
    this

  override def substituteSort(binding: SortBinding): Pattern =
    TermVar(name, sort.substituteSort(binding), typ, scope)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    typ.freshen(nameBinding).map { case (nameBinding, typ) =>
      scope.freshen(nameBinding).map { case (nameBinding, scope) =>
        if (nameBinding.contains(name)) {
          (nameBinding, TermVar(nameBinding(name), sort, typ, scope))
        } else {
          val fresh = "x" + nameProvider.next
          (nameBinding + (name -> fresh), TermVar(fresh, sort, typ, scope))
        }
      }
    }

  override def toString: String =
    s"""TermVar("$name", $sort, $typ, $scope)"""
}

case class PatternNameAdapter(n: Name) extends Pattern {
  override def vars: List[TermVar] =
    Nil

  override def points =
    ???

  override def size: Int =
    1

  override def names: List[SymbolicName] = n match {
    case s@SymbolicName(_, _) => List(s)
    case _ => Nil
  }

  override def substituteType(binding: TypeBinding): Pattern =
    this

  override def substituteName(binding: NameBinding): Pattern =
    PatternNameAdapter(n.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Pattern =
    PatternNameAdapter(n.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Pattern =
    this

  override def substituteTerm(binding: Map[TermVar, Pattern]): Pattern =
    this

  override def substituteScope(binding: ScopeBinding): Pattern =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      (nameBinding, PatternNameAdapter(n))
    }
}

// Name
abstract class Name {
  def substitute(nameBinding: Map[String, String]): Name

  def substitute(on1: Name, on2: Name): Name = this match {
    case `on1` => on2
    case _ => this
  }

  def name: String

  def namespace: String

  def substituteName(binding: NameBinding): Name

  def substituteConcrete(binding: ConcreteBinding): Name

  def unify(n: Name, nameBinding: NameBinding): Option[NameBinding]

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Name)
}

case class SymbolicName(namespace: String, name: String) extends Name {
  override def substitute(nameBinding: Map[String, String]): Name =
    SymbolicName(namespace, nameBinding.getOrElse(name, name))

  override def substituteName(binding: NameBinding): Name =
    this

  override def substituteConcrete(binding: ConcreteBinding): Name =
    binding.getOrElse(this, this)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
    if (nameBinding.contains(name)) {
      (nameBinding, SymbolicName(namespace, nameBinding(name)))
    } else {
      val fresh = "n" + nameProvider.next
      (nameBinding + (name -> fresh), SymbolicName(namespace, fresh))
    }

  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] = n match {
    case SymbolicName(`namespace`, `name`) =>
      Some(Map())
    case v@NameVar(_) =>
      v.unify(this, nameBinding)
    case _ =>
      None
  }

  override def toString: String =
    s"""SymbolicName("$namespace", "$name")"""
}

case class ConcreteName(namespace: String, name: String, pos: Int) extends Name {
  override def substitute(nameBinding: Map[String, String]): Name =
    ConcreteName(namespace, name, nameBinding.get(name + pos).map(_.toInt).getOrElse(pos))

  override def substituteName(binding: NameBinding): Name =
    this

  override def substituteConcrete(binding: ConcreteBinding): Name =
    this

  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] = n match {
    case ConcreteName(`namespace`, `name`, `pos`) =>
      Some(Map())
    case v@NameVar(_) =>
      v.unify(this, nameBinding)
    case _ =>
      None
  }

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
    if (nameBinding.contains(name + pos)) {
      (nameBinding, ConcreteName(namespace, name, nameBinding(name + pos).toInt))
    } else {
      val fresh = nameProvider.next
      (nameBinding + (name + pos -> fresh.toString), ConcreteName(namespace, name, fresh))
    }

  override def toString: String =
    s"""ConcreteName("$namespace", "$name", $pos)"""
}

case class NameVar(name: String) extends Name {
  override def namespace = ""

  override def substitute(nameBinding: Map[String, String]): Name =
    NameVar(nameBinding.getOrElse(name, name))

  override def substituteName(binding: NameBinding): Name =
    binding.getOrElse(this, this)

  override def substituteConcrete(binding: ConcreteBinding): Name =
    this

  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] =
    if (this == n) {
      Some(Map())
    } else {
      Some(Map(this -> n))
    }

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
    if (nameBinding.contains(name)) {
      (nameBinding, NameVar(nameBinding(name)))
    } else {
      val fresh = "d" + nameProvider.next
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
      val fresh = "s" + nameProvider.next
      (nameBinding + (name -> fresh), ScopeVar(fresh))
    }

  override def toString: String =
    s"""ScopeVar("$name")"""
}

// Type
trait Type {
  def substituteType(binding: TypeBinding): Type

  def substituteName(binding: NameBinding): Type

  def substituteConcrete(binding: ConcreteBinding): Type

  def unify(t: Type, typeBinding: TypeBinding = Map.empty, nameBinding: NameBinding = Map.empty): Option[(TypeBinding, NameBinding)]

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Type)
}

case class TypeAppl(name: String, children: List[Type] = Nil) extends Type {
  override def substituteType(binding: TypeBinding): Type =
    TypeAppl(name, children.map(_.substituteType(binding)))

  override def substituteName(binding: NameBinding): Type =
    TypeAppl(name, children.map(_.substituteName(binding)))

  override def substituteConcrete(binding: ConcreteBinding): Type =
    TypeAppl(name, children.map(_.substituteConcrete(binding)))

  override def unify(typ: Type, typeBinding: TypeBinding, nameBinding: NameBinding): Option[(TypeBinding, NameBinding)] = typ match {
    case c@TypeAppl(`name`, _) if children.length == c.children.length =>
      children.zip(c.children).foldLeftWhile((typeBinding, nameBinding)) {
        case ((typeBinding, nameBinding), (t1, t2)) =>
          t1.unify(t2, typeBinding, nameBinding)
      }
    case TypeVar(_) =>
      typ.unify(this, typeBinding, nameBinding)
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

  override def substituteName(binding: NameBinding): Type =
    this

  override def substituteConcrete(binding: ConcreteBinding): Type =
    this

  override def unify(typ: Type, typeBinding: TypeBinding, nameBinding: NameBinding): Option[(TypeBinding, NameBinding)] = typ match {
    case t@TypeVar(_) if typeBinding.contains(t) =>
      unify(typeBinding(t), typeBinding, nameBinding)
    case _ =>
      if (typeBinding.contains(this)) {
        typeBinding(this).unify(typ, typeBinding, nameBinding)
      } else {
        Some((typeBinding + (this -> typ), nameBinding))
      }
  }

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Type) = {
    if (nameBinding.contains(name)) {
      (nameBinding, TypeVar(nameBinding(name)))
    } else {
      val fresh = "t" + nameProvider.next
      (nameBinding + (name -> fresh), TypeVar(fresh))
    }
  }

  override def toString: String =
    s"""TypeVar("$name")"""
}

case class TypeNameAdapter(n: Name) extends Type {
  override def substituteType(binding: TypeBinding): Type =
    this

  override def substituteName(binding: NameBinding): Type =
    TypeNameAdapter(n.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Type =
    TypeNameAdapter(n.substituteConcrete(binding))

  override def unify(typ: Type, typeBinding: TypeBinding, nameBinding: NameBinding): Option[(TypeBinding, NameBinding)] = typ match {
    case TypeNameAdapter(n2) =>
      n.unify(n2, nameBinding).map { case (nameBinding) =>
        (typeBinding, nameBinding)
      }
    case _ =>
      None
  }

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Type) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      (nameBinding, TypeNameAdapter(n))
    }
}
