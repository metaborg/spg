package nl.tudelft.fragments

// Constraint
abstract class Constraint {
  def substituteType(binding: TypeBinding): Constraint

  def substituteScope(binding: ScopeBinding): Constraint

  def substituteName(binding: NameBinding): Constraint

  def substituteConcrete(binding: ConcreteBinding): Constraint

  def substituteSort(binding: SortBinding): Constraint

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint)

  def isProper: Boolean = false
}

// Facts
case class True() extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    (nameBinding, this)

  override def isProper: Boolean =
    true
}

case class Par(s1: Scope, s2: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    Par(s1.substituteScope(binding), s2.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, DirectImport(s1, s2))
      }
    }
}

// TODO: In the current version, this is "CGNamedEdge"
case class AssociatedImport(s: Scope, n: Name) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    AssociatedImport(s, n.substituteName(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    AssociatedImport(s.substituteScope(binding), n)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    AssociatedImport(s, n.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

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

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, TypeEquals(t1, t2))
      }
    }

  override def isProper =
    true
}

case class Supertype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    Supertype(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    Supertype(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Supertype(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, Supertype(t1, t2))
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

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, Subtype(t1, t2))
      }
    }

  override def isProper =
    true
}

case class Recurse(pattern: Pattern, scopes: List[Scope], typ: Option[Type], sort: Sort) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    Recurse(pattern.substituteType(binding), scopes, typ.map(_.substituteType(binding)), sort)

  override def substituteName(binding: NameBinding): Constraint =
    Recurse(pattern.substituteName(binding), scopes, typ.map(_.substituteName(binding)), sort)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    Recurse(pattern.substituteConcrete(binding), scopes, typ.map(_.substituteConcrete(binding)), sort)

  override def substituteScope(binding: ScopeBinding): Constraint =
    Recurse(pattern.substituteScope(binding), scopes.substituteScope(binding), typ, sort)

  override def substituteSort(binding: SortBinding): Constraint =
    Recurse(pattern, scopes, typ, sort.substituteSort(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, Recurse(pattern, scopes, Some(typ), sort))
          }
          .getOrElse(
            (nameBinding, Recurse(pattern, scopes, None, sort))
          )
      }
    }

  override def isProper =
    true
}
