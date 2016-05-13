package nl.tudelft.fragments.memory

import nl.tudelft.fragments.Pattern

abstract class Graph {
  def addChild(node: Node): Graph

  def `unexplored=`(unexplored: Int): Graph

  def completed: Boolean
}

// NOTE: This data structure is not functional (mutates state). Can we improve this?
case class Node(pattern: Pattern, var children: List[Node] = Nil, var unexplored: Int = 0) extends Graph {
  override def addChild(node: Node): Graph = {
    this.children = node :: children
    this
  }

  override def `unexplored=`(unexplored: Int): Graph = {
    this.unexplored = unexplored
    this
  }

  // A node is completed if it has no unexplored paths or if all its children are completed
  override def completed: Boolean =
    unexplored == 0 || children.forall(_.unexplored == 0)
}
