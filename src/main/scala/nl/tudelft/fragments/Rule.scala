package nl.tudelft.fragments

import nl.tudelft.fragments.spoofax.Signatures
import nl.tudelft.fragments.spoofax.Signatures.Decl

// Rule
case class Rule(sort: Sort, typ: Option[Type], scopes: List[Scope], state: State) {
  def mergex(recurse: Recurse, rule: Rule)(implicit signatures: List[Decl]): Option[(Rule, Map[String, String])] = {
    // Prevent naming conflicts by freshening the names in the other rule
    val (nameBinding, freshRule) = rule.freshen()

    // Unify sort, type, scope and merge the rules
    val merged = for (
      sortUnifier <- mergeSorts(recurse.sort, freshRule.sort);
      typeUnifier <- mergeTypes(recurse.typ, freshRule.typ);
      scopeUnifier <- recurse.scopes.unify(freshRule.scopes)
    ) yield {
      val merged = copy(state = state.merge(recurse, freshRule.state))
        .substituteSort(sortUnifier)
        .substituteType(typeUnifier._1)
        .substituteName(typeUnifier._2)
        .substituteScope(scopeUnifier)

      // The merge might have broken references. Restore these by adding name disequalities.
      restoreResolution(merged)
    }

    // Check consistency. E.g. the merge might have unified t1 with t2, but if t1 = Int, t2 = Bool, it's inconsistent
    merged.flatMap(rule =>
      if (Consistency.check(merged.get.state.constraints)) {
        Some((merged.get, nameBinding))
      } else {
        None
      }
    )
  }

  // Backwards compatibility
  def merge(recurse: Recurse, rule: Rule)(implicit signatures: List[Decl]): Option[Rule] = {
    mergex(recurse, rule).map(_._1)
  }

  // Merge sorts s1, s2 by unifying s2 with any of the sorts in the injection closure of s1
  def mergeSorts(s1: Sort, s2: Sort)(implicit signatures: List[Decl]): Option[SortBinding] = {
    val possibleSorts = Signatures.injectionsClosure(Set(s1))

    possibleSorts.view
      .flatMap(_.unify(s2))
      .headOption
  }

  // Merge types
  def mergeTypes(t1: Option[Type], t2: Option[Type]): Option[(TypeBinding, NameBinding)] = (t1, t2) match {
    case (None, None) =>
      Some((Map.empty[TypeVar, Type], Map.empty[NameVar, Name]))
    case (Some(x), Some(y)) =>
      x.unify(y)
    case _ =>
      None
  }

  // Fix broken references by adding name disequalities
  def restoreResolution(rule: Rule) = {
    // The merge may have broken existing resolutions, fix this
    val fixedRule = rule.state.resolution.bindings.foldLeft(rule) { case (rule, (ref, (path, dec))) =>
      // Get the declarations that `ref` may resolve to and remove declarations longer than `dec` as they don't break the resolution
      val newResolves = Graph
        .resolves(Nil, ref, rule.state.facts, rule.state.nameConstraints)
        .filter(_._2.length <= path.length)

      if (newResolves.length != 1 || (newResolves.length == 1 && newResolves.head._3 != dec)) {
        val newDisEqs = newResolves
          .filter(_._3 != dec)
          .map { case (_, _, newDec, _) => Diseq(dec, newDec) }

        rule.copy(state =
          rule.state.copy(nameConstraints =
            newDisEqs ++ rule.state.nameConstraints
          )
        )
      } else {
        rule
      }
    }

    // TODO) Sanity check: did we really restore the resolution? (Remove this code eventually)
    for ((ref, (path, dec)) <- fixedRule.state.resolution.bindings) {
      val newResolves = Graph
        .resolves(Nil, ref, fixedRule.state.facts, fixedRule.state.nameConstraints)
        .filter(_._2.length <= path.length)

      if (newResolves.length != 1 || (newResolves.length == 1 && newResolves.head._3 != dec)) {
        println(fixedRule)
        println(newResolves)

        assert(false, "Ook na fixen nog geshadowed?!")
      }
    }

    fixedRule
  }

  def recurse: List[Recurse] =
    state.constraints
      .filter(_.isInstanceOf[Recurse])
      .asInstanceOf[List[Recurse]]

  def resolutionConstraints =
    constraints
      .filter(_.isInstanceOf[Res])
      .asInstanceOf[List[Res]]

  def points: List[(Pattern, Sort, List[Scope])] =
    ???
    // TODO: Make this langauge-dependent
//    if (sort != SortAppl("ProgramDecl")) {
//      (pattern, sort, scopes) :: recurse
//    } else {
//      recurse
//    }

  // Backwards compatibility
  def constraints: List[Constraint] =
    state.constraints

  // Backwards compatibility
  def pattern: Pattern =
    state.pattern

  def substituteType(binding: TypeBinding): Rule =
    Rule(sort, typ.map(_.substituteType(binding)), scopes, state.substituteType(binding))

  def substituteName(binding: NameBinding): Rule =
    Rule(sort, typ.map(_.substituteName(binding)), scopes, state.substituteName(binding))

  def substituteScope(binding: ScopeBinding): Rule =
    Rule(sort, typ, scopes.substituteScope(binding), state.substituteScope(binding))

  def substituteSort(binding: SortBinding): Rule =
    Rule(sort.substituteSort(binding), typ, scopes, state)

  def freshen(nameBinding: Map[String, String] = Map.empty): (Map[String, String], Rule) = {
    scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
      state.freshen(nameBinding).map { case (nameBinding, state) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, Rule(sort, Some(typ), scopes, state))
          }
          .getOrElse(
            (nameBinding, Rule(sort, typ, scopes, state))
          )
      }
    }
  }

  override def toString: String =
    s"""Rule($sort, $typ, $scopes, $state)"""
}

// TODO: Backwards compatibility
object Rule {
  def apply(pattern: Pattern, sort: Sort, typ: Type, scopes: List[Scope], state: State): Rule =
    Rule(sort, Some(typ), scopes, state.copy(pattern = pattern))
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
    ??? // vars.map(hole => (hole, hole.sort, hole.scope))

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

case class TermVar(name: String) extends Pattern {
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
    TermVar(name)

  override def substituteScope(binding: ScopeBinding): Pattern =
    TermVar(name)

  override def substituteName(binding: NameBinding): Pattern =
    this

  override def substituteConcrete(binding: ConcreteBinding): Pattern =
    this

  override def substituteSort(binding: SortBinding): Pattern =
    TermVar(name)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Pattern) =
    if (nameBinding.contains(name)) {
      (nameBinding, TermVar(nameBinding(name)))
    } else {
      val fresh = "x" + nameProvider.next
      (nameBinding + (name -> fresh), TermVar(fresh))
    }

  override def toString: String =
    s"""TermVar("$name")"""
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

  // Check if t occurs in this. In practice, this is not necessary, as fragments to be merged are always isolated.
  def occurs(t: Type): Boolean
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

  override def occurs(t: Type): Boolean =
    this == t || children.exists(_.occurs(t))
}

case class TypeVar(name: String) extends Type {
  override def substituteType(binding: TypeBinding): Type =
    binding.getOrElse(this, this)

  override def substituteName(binding: NameBinding): Type =
    this

  override def substituteConcrete(binding: ConcreteBinding): Type =
    this

  override def unify(typ: Type, typeBinding: TypeBinding, nameBinding: NameBinding): Option[(TypeBinding, NameBinding)] = {
    // TODO: Debug StackOverflowError due to TypeVar occurring in the thing you're merging with..
    if (this.occurs(typ)) {
      println(this)
      println(typ)
      System.exit(1)
    }

    typ match {
      case t@TypeVar(_) if typeBinding.contains(t) =>
        unify(typeBinding(t), typeBinding, nameBinding)
      case _ =>
        if (typeBinding.contains(this)) {
          typeBinding(this).unify(typ, typeBinding, nameBinding)
        } else {
          Some((typeBinding + (this -> typ), nameBinding))
        }
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

  override def occurs(t: Type): Boolean =
    this == t
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

  override def occurs(t: Type): Boolean =
    this == t
}
