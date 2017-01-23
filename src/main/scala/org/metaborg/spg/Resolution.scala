package org.metaborg.spg

case class Resolution(bindings: Map[Pattern, Pattern] = Map.empty) {
  def contains(n: Pattern): Boolean =
    bindings.contains(n)

  def apply(n: Pattern) =
    bindings(n)

  def get(n: Pattern) =
    bindings.get(n)

  def +(e: (Pattern, Pattern)) =
    Resolution(bindings + e)

  def ++(resolution: Resolution) =
    Resolution(bindings ++ resolution.bindings)

  def substitute(binding: TermBinding): Resolution =
    Resolution(
      bindings.map { case (t1, t2) =>
        t1.substitute(binding) -> t2.substitute(binding)
      }
    )

  def size =
    bindings.size

  def freshen(nameBinding: Map[String, String]): (Map[String, String], Resolution) = {
    val freshBindings = bindings.toList.mapFoldLeft(nameBinding) { case (nameBinding, (n1, n2)) =>
      n1.freshen(nameBinding).map { case (nameBinding, n1) =>
        n2.freshen(nameBinding).map { case (nameBinding, n2) =>
          (nameBinding, n1 -> n2)
        }
      }
    }

    freshBindings.map { case (nameBinding, bindings) =>
      (nameBinding, Resolution(bindings.toMap))
    }
  }

  override def toString =
    "Resolution(Map(" + bindings.map { case (n1, n2) => s"Tuple2($n1, $n2)" }.mkString(", ") + "))"
}
