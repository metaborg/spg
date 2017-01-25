package org.metaborg.spg.collection

import scala.collection.mutable

/**
  * A disjoint-set (union-find) data structure backed by a HashMap.
  */
class DisjointSet[A] {
  import DisjointSet.Node
  private[this] val parent = mutable.Map.empty[A, Node[A]]

  private[this] implicit def toNode(x: A): Node[A] = {
    assume(contains(x))
    parent(x)
  }

  /**
    * @return true iff x is known
    */
  def contains(x: A) = parent contains x

  /**
    * Add a new singleton set with only x in it (assuming x is not already known)
    */
  def +=(x: A) = {
    assume(!contains(x))
    parent(x) = new Node(x)
  }

  /**
    * Union the sets containing x and y
    */
  def union(x: A, y: A) = {
    val (xRoot, yRoot) = (x.root, y.root)
    if (xRoot != yRoot) {
      if (xRoot.rank < yRoot.rank) { // change the root of the shorter/less-depth one
        xRoot.parent = yRoot
      } else if (xRoot.rank > yRoot.rank) {
        yRoot.parent = xRoot
      } else {
        yRoot.parent = xRoot
        xRoot.rank += 1 // else if there is tie, increment
      }
    }
  }

  /**
    * @return the root (or the canonical element that contains x)
    */
  def apply(x: A) = x.root.entry

  /**
    * Get the roots, i.e. representatives of each set
    *
    * @return
    */
  def roots: List[A] = parent.keys.map(_.root.entry).toList.distinct

  /**
    * @return Iterator over groups of items in same set
    */
  def sets: Set[Set[A]] = parent.keys.toSet[A].groupBy((a: A) => a.root.entry).values.toSet[Set[A]]

  /**
    * Two DisjointSet objects are equal if their group of sets are equal.
    *
    * @param other
    * @return
    */
  override def equals(other: Any): Boolean = other match {
    case that: DisjointSet[A] =>
      sets == that.sets
    case _ =>
      false
  }

  override def toString: String =
    s"DisjointSet($sets)"
}

object DisjointSet {
  /**
    * Each internal node in DisjointSet
    */
  private[DisjointSet] class Node[A](val entry: A) {
    /**
      * parent - the pointer to root node (by default itself)
      * rank - depth if we did not do path compression in find - else its upper bound on the distance from node to parent
      */
    var (parent, rank) = (this, 0)

    def root: Node[A] = {
      if (parent != this) {
        parent = parent.root // path compression
      }
      parent
    }
  }

  /**
    * @return empty disjoint set
    */
  def empty[A] = new DisjointSet[A]

  /**
    * @return a disjoint set with each element in its own set
    */
  def apply[A](elements: A*) = {
    val d = empty[A]
    elements foreach {e => d += e}
    d
  }
}
