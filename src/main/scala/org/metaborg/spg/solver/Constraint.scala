package org.metaborg.spg.solver

import org.metaborg.spg._
import org.metaborg.spg.resolution.Label
import org.metaborg.spg.spoofax.models.Sort

// Constraint
abstract class Constraint {
  def substitute(binding: TermBinding): Constraint

  /**
    * Substitute a scope variable by an application
    *
    * @param binding
    * @return
    */
  def substituteScope(binding: TermBinding): Constraint

  /**
    * Substitute a type variable by an application
    *
    * @param binding
    * @return
    */
  def substituteType(binding: TermBinding): Constraint

  def substituteSort(binding: SortBinding): Constraint

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint)

  def isProper: Boolean = false

  def priority: Int = 99
}

// Facts
case class CTrue() extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    this

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    (nameBinding, this)

  override def isProper: Boolean =
    true

  override def priority =
    0
}

/**
  * An labeled edge between two scopes. Both scopes can be non-ground.
  *
  * @param s1
  * @param l
  * @param s2
  */
case class CGDirectEdge(s1: Pattern, l: Label, s2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGDirectEdge(s1.substitute(binding), l, s2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    CGDirectEdge(s1.substituteScope(binding), l, s2.substituteScope(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s1.freshen(nameBinding).map { case (nameBinding, s1) =>
      s2.freshen(nameBinding).map { case (nameBinding, s2) =>
        (nameBinding, CGDirectEdge(s1, l, s2))
      }
    }

  override def priority =
    3
}

/**
  * A declaration in a scope. The scope will be non-ground until the rule is
  * instantiated.
  *
  * @param s
  * @param n
  */
case class CGDecl(s: Pattern, n: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGDecl(s.substitute(binding), n.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    CGDecl(s.substituteScope(binding), n)

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGDecl(s, n))
      }
    }

  override def priority =
    3
}

/**
  * A reference in a scope. The scope will be non-ground until the rule is
  * instantiated. After instantiation, the scope can still be non-ground.
  *
  * @param n
  * @param s
  */
case class CGRef(n: Pattern, s: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGRef(n.substitute(binding), s.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    CGRef(n, s.substituteScope(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGRef(n, s))
      }
    }

  override def priority =
    3
}

case class CGNamedEdge(s: Pattern, l: Label, n: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGNamedEdge(s, l, n.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    CGNamedEdge(s, l, n.substituteScope(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    s.freshen(nameBinding).map { case (nameBinding, s) =>
      n.freshen(nameBinding).map { case (nameBinding, n) =>
        (nameBinding, CGNamedEdge(s, l, n))
      }
    }

  override def priority =
    3
}

/**
  * Associate a name to a scope. The scope will not be ground until the rule is
  * instantiated.
  *
  * @param n
  * @param s
  */
case class CGAssoc(n: Pattern, s: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGAssoc(n.substitute(binding), s)

  override def substituteScope(binding: TermBinding): Constraint =
    CGAssoc(n, s.substituteScope(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n.freshen(nameBinding).map { case (nameBinding, n) =>
      s.freshen(nameBinding).map { case (nameBinding, s) =>
        (nameBinding, CGAssoc(n, s))
      }
    }

  override def priority =
    3
}

/**
  * Associate a name to a scope. The scope can be non-ground.
  *
  * @param n
  * @param s
  */
case class CAssoc(n: Pattern, s: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CAssoc(n.substitute(binding), s)

  override def substituteScope(binding: TermBinding): Constraint =
    CAssoc(n, s.substituteScope(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

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

  override def priority =
    4
}

/**
  * Resolve a reference to a declaration. The declaration can be non-ground.
  *
  * @param n1
  * @param n2
  */
case class CResolve(n1: Pattern, n2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CResolve(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

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

  override def priority =
    999
}

/**
  * Associate a type to an occurrence. Both arguments can be non-ground.
  *
  * @param n
  * @param t
  */
case class CTypeOf(n: Pattern, t: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CTypeOf(n.substitute(binding), t.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    CTypeOf(n, t.substituteType(binding))

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

  override def priority =
    1
}

case class CEqual(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CEqual(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    CEqual(t1.substituteType(binding), t2.substituteType(binding))

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

  override def priority =
    0
}

case class CInequal(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CInequal(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    CInequal(t1.substituteType(binding), t2.substituteType(binding))

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    t1.freshen(nameBinding).map { case (nameBinding, t1) =>
      t2.freshen(nameBinding).map { case (nameBinding, t2) =>
        (nameBinding, CInequal(t1, t2))
      }
    }

  override def isProper =
    true

  override def priority =
    99
}

case class FSubtype(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    FSubtype(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    FSubtype(t1.substituteType(binding), t2.substituteType(binding))

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

  override def priority =
    0
}

case class CSubtype(t1: Pattern, t2: Pattern) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CSubtype(t1.substitute(binding), t2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    CSubtype(t1.substituteType(binding), t2.substituteType(binding))

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

  override def priority =
    6
}

case class CGenRecurse(name: String, pattern: Pattern, scopes: List[Pattern], typ: Option[Pattern], sort: Sort) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CGenRecurse(name, pattern.substitute(binding), scopes.map(_.substitute(binding)), typ.map(_.substitute(binding)), sort)

  override def substituteScope(binding: TermBinding): Constraint =
    CGenRecurse(name, pattern, scopes.map(_.substituteScope(binding)), typ.map(_.substituteScope(binding)), sort)

  override def substituteType(binding: TermBinding): Constraint =
    CGenRecurse(name, pattern, scopes, typ.map(_.substituteType(binding)), sort)

  override def substituteSort(binding: SortBinding): Constraint =
    CGenRecurse(name, pattern, scopes, typ, sort.substituteSort(binding))

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    pattern.freshen(nameBinding).map { case (nameBinding, pattern) =>
      scopes.freshen(nameBinding).map { case (nameBinding, scopes) =>
        val newTyp = typ.map(_.freshen(nameBinding))

        newTyp
          .map { case (nameBinding, typ) =>
            (nameBinding, CGenRecurse(name, pattern, scopes, Some(typ), sort))
          }
          .getOrElse(
            (nameBinding, CGenRecurse(name, pattern, scopes, None, sort))
          )
      }
    }

  override def isProper =
    true

  override def priority: Int =
    999

  override def toString: String =
    s"""CGenRecurse("$name", $pattern, $scopes, $typ, $sort)"""
}

case class CFalse() extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    this

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    (nameBinding, this)

  override def isProper =
    true

  override def priority =
    0
}

case class CDistinct(names: Names) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    CDistinct(names.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    CDistinct(names.substitute(binding))

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    names.freshen(nameBinding).map { case (nameBinding, names) =>
      (nameBinding, CDistinct(names))
    }

  override def isProper =
    true

  /**
    * Distinct has the highest priority. For generation, solving CDistinct does
    * not uncover new information and it can only be solved when the scope is
    * ground.
    */
  override def priority =
    99
}

case class NewScope(v: Var) extends Constraint {
  override def substitute(binding: TermBinding): Constraint =
    this

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    (nameBinding, this)
}

abstract class Names {
  def substitute(binding: TermBinding): Names

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Names)
}

case class Declarations(scope: Pattern, namespace: String) extends Names {
  override def substitute(binding: TermBinding): Names =
    Declarations(scope.substitute(binding), namespace)

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Names) =
    scope.freshen(nameBinding).map { case (nameBinding, scope) =>
      (nameBinding, Declarations(scope, namespace))
    }

  override def toString: String =
    s"""Declarations($scope, "$namespace")"""
}

abstract class NamingConstraint extends Constraint

case class Diseq(n1: Pattern, n2: Pattern) extends NamingConstraint {
  override def substitute(binding: TermBinding): Constraint =
    Diseq(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Diseq(n1, n2))
      }
    }
}

case class Eq(n1: Pattern, n2: Pattern) extends NamingConstraint {
  override def substitute(binding: TermBinding): Constraint =
    Eq(n1.substitute(binding), n2.substitute(binding))

  override def substituteScope(binding: TermBinding): Constraint =
    this

  override def substituteType(binding: TermBinding): Constraint =
    this

  override def substituteSort(binding: SortBinding): Constraint =
    this

  override def freshen(nameBinding: Map[String, String]): (Map[String, String], Constraint) =
    n1.freshen(nameBinding).map { case (nameBinding, n1) =>
      n2.freshen(nameBinding).map { case (nameBinding, n2) =>
        (nameBinding, Eq(n1, n2))
      }
    }
}
