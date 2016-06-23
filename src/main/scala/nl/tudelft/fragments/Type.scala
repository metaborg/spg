package nl.tudelft.fragments

trait Type {
  def substituteType(binding: TypeBinding): Type

  def substituteName(binding: NameBinding): Type

  def substituteConcrete(binding: ConcreteBinding): Type

  def unify(t: Type, typeBinding: TypeBinding = Map.empty, nameBinding: NameBinding = Map.empty): Option[(TypeBinding, NameBinding)]

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Type)

  // Get the variables in this type
  def vars: List[TypeVar]

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

  override def vars: List[TypeVar] =
    children.flatMap(_.vars)
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

  override def vars: List[TypeVar] =
    List(this)
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

  override def vars: List[TypeVar] =
    Nil
}
