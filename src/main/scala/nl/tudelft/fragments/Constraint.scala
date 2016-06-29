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
case class CTrue() extends Constraint {
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

case class CGDirectEdge(s1: Scope, l: Label, s2: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGDirectEdge(s1.substituteScope(binding), l, s2.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    this

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, CGDirectEdge(s1, l, s2))
      }
    }
}

case class CGDecl(s: Scope, n: Name) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGDecl(s.substituteScope(binding), n)

  override def substituteName(binding: NameBinding): Constraint =
    CGDecl(s, n.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CGDecl(s, n.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGDecl(s, n))
      }
    }
}

case class CGRef(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGRef(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    CGRef(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CGRef(n.substituteConcrete(binding), s)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGRef(n, s))
      }
    }
}

case class CGNamedEdge(s: Scope, l: Label, n: Name) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    CGNamedEdge(s, l, n.substituteName(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGNamedEdge(s.substituteScope(binding), l, n)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CGNamedEdge(s, l, n.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGNamedEdge(s, l, n))
      }
    }
}

case class CGAssoc(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGAssoc(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    CGAssoc(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CGAssoc(n.substituteConcrete(binding), s)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGAssoc(n, s))
      }
    }
}

// Proper constraints
case class CAssoc(n: Name, s: Scope) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CAssoc(n, s.substituteScope(binding))

  override def substituteName(binding: NameBinding): Constraint =
    CAssoc(n.substituteName(binding), s)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CAssoc(n.substituteConcrete(binding), s)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CAssoc(n, s))
      }
    }

  override def isProper =
    true
}

case class CResolve(n1: Name, n2: Name) extends Constraint {
  def substitute(nameBinding: Map[String, String]) =
    CResolve(n1.substitute(nameBinding), n2.substitute(nameBinding))

  override def substituteType(binding: TypeBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CResolve(n1, n2)

  override def substituteName(binding: NameBinding): Constraint =
    CResolve(n1.substituteName(binding), n2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CResolve(n1.substituteConcrete(binding), n2.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], CResolve) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, CResolve(n1, n2))
      }
    }

  override def isProper =
    true
}

case class CTypeOf(n: Name, t: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    CTypeOf(n, t.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    CTypeOf(n.substituteName(binding), t.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CTypeOf(n.substituteConcrete(binding), t)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t.freshen(nameBinding).map { case (nameBinding, t) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CTypeOf(n, t))
      }
    }

  override def isProper =
    true
}

case class CEqual(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    CEqual(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    CEqual(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CEqual(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, CEqual(t1, t2))
      }
    }

  override def isProper =
    true
}

case class FSubtype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    FSubtype(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    FSubtype(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    FSubtype(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, FSubtype(t1, t2))
      }
    }

  override def isProper =
    true
}

case class CSubtype(t1: Type, t2: Type) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    CSubtype(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteName(binding: NameBinding): Constraint =
    CSubtype(t1.substituteName(binding), t2.substituteName(binding))

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CSubtype(t1.substituteConcrete(binding), t2.substituteConcrete(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, CSubtype(t1, t2))
      }
    }

  override def isProper =
    true
}

case class CGenRecurse(pattern: Pattern, scopes: List[Scope], typ: Option[Type], sort: Sort) extends Constraint {
  override def substituteType(binding: TypeBinding): Constraint =
    CGenRecurse(pattern.substituteType(binding), scopes, typ.map(_.substituteType(binding)), sort)

  override def substituteName(binding: NameBinding): Constraint =
    CGenRecurse(pattern.substituteName(binding), scopes, typ.map(_.substituteName(binding)), sort)

  override def substituteConcrete(binding: ConcreteBinding): Constraint =
    CGenRecurse(pattern.substituteConcrete(binding), scopes, typ.map(_.substituteConcrete(binding)), sort)

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGenRecurse(pattern.substituteScope(binding), scopes.substituteScope(binding), typ, sort)

  override def substituteSort(binding: SortBinding): Constraint =
    CGenRecurse(pattern, scopes, typ, sort.substituteSort(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, CGenRecurse(pattern, scopes, Some(typ), sort))
          }
          .getOrElse(
            (nameBinding, CGenRecurse(pattern, scopes, None, sort))
          )
      }
    }

  override def isProper =
    true
}
