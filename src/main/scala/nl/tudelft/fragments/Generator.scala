package nl.tudelft.fragments

//import nl.tudelft.fragments.memory.{Graph, Node}
//
//import scala.util.Random
//
//// Top-down generator
//object Generator {
//  def generate(rules: List[Rule], current: Rule, maxSize: Int, parent: Node, graph: Graph): Option[(Rule, Node)] =
//    if (maxSize < 0) {
//      None
//    } else {
//      val holes = current.pattern.vars
//
//      if (holes.isEmpty) {
//        Some((current, parent))
//      } else {
//
//        //------------------------------------------------------------------------------------------
//        // With memoization
//
//        // For every hole
//        val unexplored = for (hole <- holes) yield {
//          // Compute applicable rules
//          val applicable = rules
//            // Filter and instantiate (polymorphic sorts) applicable rules
//            .flatMap(rule => rule.sort.unify(hole.sort).map(rule.substituteSort))
//            // Filter rules that are not too large
//            .filter(_.pattern.vars.length < maxSize/holes.length)
//
//          // Add all choices to generation graph and yield choices that lead to unexplored paths
//          for (rule <- applicable) yield {
//            val merged = current.merge(hole, rule)
//
//            // Check if merged.pattern occurs *as a child of this parent* in the graph (TODO modulo names, if we want "real" graph merging?)
//            val node = if (parent.children.exists(_.pattern == merged.pattern)) {
//              parent.children.find(_.pattern == merged.pattern).get
//            } else {
//              val node = Node(merged.pattern, Nil, 1)
//              parent.addChild(node)
//              node
//            }
//
//            if (node.unexplored > 0) {
//              Some((node, merged))
//            } else {
//              None
//            }
//          }
//        }
//
//        // Shuffle the nodes
//        val random = Random.shuffle(unexplored.flatten.flatten)
//
//        // Option a) Backtracking (depth-first)
//        //        for ((node, rule) <- random) {
//
//        // Option b) Pick random and fail early
//        if (random.nonEmpty) {
//          val (node, rule) = random.random
//
//          // Check if the result is consistent
//          if (Consistency.check(rule.constraints)) {
//            // Continue generation
//            val complete = generate(rules, rule, maxSize-(rule.pattern.size-current.pattern.size), node, graph)
//
//            // Cache that a node is completed by setting its unexplored to 0
//            if (node.completed) {
//              node.unexplored = 0
//            }
//
//            if (complete.isDefined) {
//              return complete
//            }
//          }
//        }
//
//        None
//
//        //------------------------------------------------------------------------------------------
//        // Without memoization
//
//        /*
//        // Pick a random hole
//        val hole = holes.random
//
//        // Compute applicable rules
//        val applicable = rules
//          // Filter and instantiate (polymorphic sorts) applicable rules
//          .flatMap(rule => rule.sort.unify(hole.sort).map(rule.substituteSort))
//          // Filter rules that are not too large
//          .filter(_.pattern.vars.length < maxSize/holes.length)
//
//        // Shuffle the nodes
//        val random = Random.shuffle(applicable)
//
//        // Try each rule, i.e. depth-first
//        for (rule <- random) {
//          // Merge rule into current at hole
//          val merged = current.merge(hole, rule)
//
//          // Check if the result is consistent
//          if (Consistency.check(merged.constraints)) {
//            i = i+1
//
//            val complete = generate(rules, merged, maxSize - (merged.pattern.size - current.pattern.size), parent, graph)
//
//            if (complete.isDefined) {
//              return complete
//            }
//          }
//        }
//
//        None
//        */
//      }
//    }
//}
