package nl.tudelft.fragments

//// Name
//abstract class Name {
//  def substitute(nameBinding: Map[String, String]): Name
//
//  def substitute(on1: Name, on2: Name): Name = this match {
//    case `on1` => on2
//    case _ => this
//  }
//
//  def name: String
//
//  def namespace: String
//
//  def vars: List[NameVar]
//
//  def substituteName(binding: NameBinding): Name
//
//  def substituteConcrete(binding: ConcreteBinding): Name
//
//  def unify(n: Name, nameBinding: NameBinding): Option[NameBinding]
//
//  def freshen(nameBinding: Map[String, String]): (Map[String, String], Name)
//}
//
//case class SymbolicName(namespace: String, name: String) extends Name {
//  override def substitute(nameBinding: Map[String, String]): Name =
//    SymbolicName(namespace, nameBinding.getOrElse(name, name))
//
//  override def substituteName(binding: NameBinding): Name =
//    this
//
//  override def substituteConcrete(binding: ConcreteBinding): Name =
//    binding.getOrElse(this, this)
//
//  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
//    if (nameBinding.contains(name)) {
//      (nameBinding, SymbolicName(namespace, nameBinding(name)))
//    } else {
//      val fresh = "n" + nameProvider.next
//      (nameBinding + (name -> fresh), SymbolicName(namespace, fresh))
//    }
//
//  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] = n match {
//    case SymbolicName(`namespace`, `name`) =>
//      Some(Map())
//    case v@NameVar(_) =>
//      v.unify(this, nameBinding)
//    case _ =>
//      None
//  }
//
//  override def vars: List[NameVar] =
//    Nil
//
//  override def toString: String =
//    s"""SymbolicName("$namespace", "$name")"""
//}
//
//case class ConcreteName(namespace: String, name: String, pos: Int) extends Name {
//  override def substitute(nameBinding: Map[String, String]): Name =
//    ConcreteName(namespace, name, nameBinding.get(name + pos).map(_.toInt).getOrElse(pos))
//
//  override def substituteName(binding: NameBinding): Name =
//    this
//
//  override def substituteConcrete(binding: ConcreteBinding): Name =
//    this
//
//  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] = n match {
//    case ConcreteName(`namespace`, `name`, `pos`) =>
//      Some(Map())
//    case v@NameVar(_) =>
//      v.unify(this, nameBinding)
//    case _ =>
//      None
//  }
//
//  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
//    if (nameBinding.contains(name + pos)) {
//      (nameBinding, ConcreteName(namespace, name, nameBinding(name + pos).toInt))
//    } else {
//      val fresh = nameProvider.next
//      (nameBinding + (name + pos -> fresh.toString), ConcreteName(namespace, name, fresh))
//    }
//
//  override def vars: List[NameVar] =
//    Nil
//
//  override def toString: String =
//    s"""ConcreteName("$namespace", "$name", $pos)"""
//}
//
//case class NameVar(name: String) extends Name {
//  override def namespace = ""
//
//  override def substitute(nameBinding: Map[String, String]): Name =
//    NameVar(nameBinding.getOrElse(name, name))
//
//  override def substituteName(binding: NameBinding): Name =
//    binding.getOrElse(this, this)
//
//  override def substituteConcrete(binding: ConcreteBinding): Name =
//    this
//
//  override def unify(n: Name, nameBinding: NameBinding): Option[NameBinding] =
//    if (this == n) {
//      Some(Map())
//    } else {
//      Some(Map(this -> n))
//    }
//
//  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Name) =
//    if (nameBinding.contains(name)) {
//      (nameBinding, NameVar(nameBinding(name)))
//    } else {
//      val fresh = "d" + nameProvider.next
//      (nameBinding + (name -> fresh), NameVar(fresh))
//    }
//
//  override def vars: List[NameVar] =
//    List(this)
//
//  override def toString: String =
//    s"""NameVar("$name")"""
//}
