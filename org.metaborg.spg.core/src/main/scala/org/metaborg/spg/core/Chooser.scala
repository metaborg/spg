package org.metaborg.spg.core

import org.metaborg.spg.core.solver.Constraint

import scala.io.StdIn._

trait Chooser {
  /**
    * Get a list of constraints to solve.
    *
    * @param program
    * @return
    */
  def nextConstraints(program: Program): List[Constraint]

  /**
    * Get a list of programs to continue with.
    *
    * @param programs
    * @return
    */
  def nextProgram(programs: List[Program]): List[Program]
}

/**
  * The interactive strategy lets the user choose the next constraint and next
  * program. This is useful during debugging, if you manually want to force
  * a specific set of choices.
  */
class InteractiveChooser extends Chooser {
  /**
    * Get a list of constraints to solve.
    *
    * @param program
    * @return
    */
  override def nextConstraints(program: Program): List[Constraint] = {
    val sortedConstraints = program.properConstraints.shuffle.sortBy(_.priority)

    for ((constraint, index) <- sortedConstraints.zipWithIndex) {
      println(s"[$index]: $constraint")
    }

    List(sortedConstraints(readInt()))
  }

  /**
    * Get a list of programs to continue with.
    *
    * @param programs
    * @return
    */
  override def nextProgram(programs: List[Program]): List[Program] = {
    for ((program, index) <- programs.zipWithIndex) {
      println(s"[$index]: $program")
    }

    List(programs(readInt()))
  }
}

class AutomaticChooser extends Chooser {
  /**
    * Get a list of constraints to solve.
    *
    * @param program
    * @return
    */
  override def nextConstraints(program: Program): List[Constraint] = {
    program
      .properConstraints
      .shuffle
      .sortBy(_.priority)
  }

  /**
    * Get a list of programs to continue with.
    *
    * @param programs
    * @return
    */
  override def nextProgram(programs: List[Program]): List[Program] = {
    programs.shuffle
  }
}
