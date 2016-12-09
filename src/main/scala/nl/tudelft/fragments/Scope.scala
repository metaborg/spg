package nl.tudelft.fragments

abstract class Scope {
  def name: String

  def unify(t: Scope, binding: ScopeBinding = Map.empty): Option[ScopeBinding]

  def substitute(binding: TermBinding): Scope

  def substituteScope(binding: ScopeBinding): Scope

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Scope)

  def vars: List[ScopeVar]
}

case class ScopeAppl(name: String) extends Scope {
  override def unify(scope: Scope, binding: ScopeBinding): Option[ScopeBinding] = scope match {
    case ScopeAppl(n) =>
      Some(binding + (this -> scope))
    case ScopeVar(_) =>
      scope.unify(this, binding)
    case _ =>
      None
  }

  override def substitute(binding: TermBinding): Scope =
    binding.getOrElse()

  override def substituteScope(binding: ScopeBinding): Scope =
    binding.getOrElse(this, this)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Scope) =
    if (nameBinding.contains(name)) {
      (nameBinding, ScopeAppl(nameBinding(name)))
    } else {
      val fresh = "s" + nameProvider.next
      (nameBinding + (name -> fresh), ScopeAppl(fresh))
    }

  override def vars: List[ScopeVar] =
    Nil

  override def toString: String =
    s"""ScopeAppl("$name")"""
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

  override def vars: List[ScopeVar] =
    List(this)

  override def toString: String =
    s"""ScopeVar("$name")"""
}
