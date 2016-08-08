package nl.tudelft.fragments

// Constraint
abstract class Constraint {
  def substitute(binding: TermBinding): Constraint

  def substituteScope(binding: ScopeBinding): Constraint

  def substituteSort(binding: SortBinding): Constraint

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint)

  def isProper: Boolean = false
}

// Facts
case class CTrue() extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    (nameBinding, this)

  override def isProper: Boolean =
    true
}

case class CGDirectEdge(s1: Scope, l: Label, s2: Scope) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    this

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGDirectEdge(s1.substituteScope(binding), l, s2.substituteScope(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, CGDirectEdge(s1, l, s2))
      }
    }
}

case class CGDecl(s: Scope, n: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGDecl(s, n.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGDecl(s.substituteScope(binding), n)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGDecl(s, n))
      }
    }
}

case class CGRef(n: Pattern, s: Scope) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGRef(n.substitute(binding), s)

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGRef(n, s.substituteScope(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGRef(n, s))
      }
    }
}

case class CGNamedEdge(s: Scope, l: Label, n: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGNamedEdge(s, l, n.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGNamedEdge(s.substituteScope(binding), l, n)

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGNamedEdge(s, l, n))
      }
    }
}

case class CGAssoc(n: Pattern, s: Scope) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGAssoc(n.substitute(binding), s)

  override def substituteScope(binding: ScopeBinding): Constraint =
    CGAssoc(n, s.substituteScope(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGAssoc(n, s))
      }
    }
}

case class CAssoc(n: Pattern, s: Scope) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CAssoc(n.substitute(binding), s)

  override def substituteScope(binding: ScopeBinding): Constraint =
    CAssoc(n, s.substituteScope(binding))

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

case class CResolve(n1: Pattern, n2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CResolve(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    CResolve(n1, n2)

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

case class CTypeOf(n: Pattern, t: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CTypeOf(n.substitute(binding), t.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

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

case class CEqual(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CEqual(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

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

case class FSubtype(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    FSubtype(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

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

case class CSubtype(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CSubtype(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: ScopeBinding): Constraint =
    this

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

case class CGenRecurse(pattern: Pattern, scopes: List[Scope], typ: Option[Pattern], sort: Sort) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGenRecurse(pattern.substitute(binding), scopes, typ.map(_.substitute(binding)), sort)

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
