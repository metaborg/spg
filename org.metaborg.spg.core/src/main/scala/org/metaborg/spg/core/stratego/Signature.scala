package org.metaborg.spg.core.stratego

import org.metaborg.spg.core.fixedPoint
import org.metaborg.spg.core.sdf.Sort
import org.metaborg.spg.core.terms.{As, Pattern, TermAppl, Var}

import scala.collection.mutable

/**
  * An (algebraic) signature consists of a list of constructors.
  *
  * @param constructors
  */
case class Signature(constructors: List[Constructor]) {
  /**
    * Get only the operations from the list of constructors.
    */
  lazy val operations: List[Operation] = constructors.collect {
    case x: Operation =>
      x
  }

  /**
    * Get only the injections from the list of constructors.
    */
  lazy val injections: List[Injection] = constructors.collect {
    case x: Injection =>
      x
  }

  /**
    * Get all distinct sorts in the signature.
    */
  lazy val sorts: List[Sort] = {
    constructors.flatMap(_.sorts).distinct
  }

  /**
    * Get the operations for the given sort.
    *
    * @param sort
    * @return
    */
  def getOperations(sort: Sort): List[Operation] = {
    operations.flatMap(operation =>
      operation.target.unify(sort).map(unifier =>
        operation.substitute(unifier)
      )
    )
  }

  /**
    * Get the operations for both the given sort and all its injections.
    *
    * @param sort
    * @return
    */
  def getOperationsTransitive(sort: Sort): List[Operation] = {
    injectionsClosure(sort).flatMap(getOperations).toList
  }

  /**
    * Memoized version of getOperationsTransitive.
    *
    * @return
    */
  val getOperationsTransitiveMem: Sort => List[Operation] = {
    val memory = mutable.Map.empty[Sort, List[Operation]]

    (s: Sort) => memory.getOrElseUpdate(s, getOperationsTransitive(s))
  }

  /**
    * Get the constructors based on the given pattern.
    *
    * @param pattern
    * @return
    */
  def getOperations(pattern: Pattern): List[Operation] = {
    pattern match {
      case TermAppl(cons, children) =>
        operations
          .filter(_.name == cons)
          .filter(_.arity == children.size)
      case _ =>
        Nil
    }
  }

  /**
    * Get the sort for the given pattern in the given context.
    *
    *
    * @param pattern
    * @param context
    * @return
    */
  def getSort(context: Pattern, pattern: Pattern): Option[Sort] = {
    // TODO: I do not really understand this method, but it seems to work?

    sortForPattern(context, pattern)
  }

  def sortForPattern(context: Pattern, pattern: Pattern, sort: Option[Sort] = None): Option[Sort] = (context, pattern) match {
    case (_, _) if context == pattern =>
      sort
    case (As(Var(n1), _), Var(n2)) if n1 == n2 =>
      sort
    case (As(Var(n1), term1), term2) =>
      sortForPattern(term1, term2)
    case (TermAppl(_, children), _) =>
      val operation = getOperations(context).head
      val sorts = operation.arguments

      (children, sorts).zipped.foldLeft(Option.empty[Sort]) {
        case (Some(x), _) =>
          Some(x)
        case (_, (child, sort)) =>
          sortForPattern(child, pattern, Some(sort))
      }
    case _ =>
      None
  }

  /**
    * Get the direct injections for the given sort.
    *
    * Injections can be parametric, so for every injection the given sort is
    * unified with the target sort and this unifier is applied to the source
    * sort.
    *
    * @param sort
    * @return
    */
  def injections(sort: Sort): Set[Sort] = {
    val sorts = injections.flatMap(injection =>
      injection.target.unify(sort).map(injection.argument.substituteSort)
    )

    sorts.toSet
  }

  /**
    * Get the direct injections for the given set of sorts.
    *
    * @param sorts
    * @return
    */
  def injections(sorts: Set[Sort]): Set[Sort] = {
    sorts.flatMap(injections(_)) ++ sorts
  }

  /**
    * Get the transitive closure of the injection relation on the given set
    * of sorts.
    *
    * @param sorts
    * @return
    */
  def injectionsClosure(sorts: Set[Sort]): Set[Sort] = {
    fixedPoint(injections(_: Set[Sort]), sorts)
  }

  /**
    * Compute the transitive closure of the injection relation on the given sort.
    *
    * @param sort
    * @return
    */
  def injectionsClosure(sort: Sort): Set[Sort] = {
    injectionsClosure(Set(sort))
  }
}
