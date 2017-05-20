package org.metaborg.spg.core.sdf

import org.metaborg.spg.core.stratego.Constructor
import org.metaborg.spg.core._

/**
  * A representation of an SDF grammar.
  *
  * @param modules
  */
case class Grammar(modules: Seq[Module]) {
  /**
    * Get all contstructors for all modules.
    *
    * @return
    */
  def toConstructors: Seq[Constructor] = {
    modules.flatMap(_.toConstructors)
  }

  /**
    * Get all productions (context-free, lexical, and kernel) for all modules.
    *
    * @return
    */
  def productions: Seq[Production] = {
    modules.flatMap(_.productions)
  }

  /**
    * Get a module by its name.
    *
    * @param name
    * @return
    */
  def getModule(name: String): Module = {
    val moduleOpt = modules.find(_.name == name)

    moduleOpt match {
      case Some(m) =>
        m
      case _ =>
        throw new IllegalArgumentException(s"SDF module with name '$name' not found.")
    }
  }

  /**
    * Compute the effective subgrammar of the grammar based on the given entry
    * point.
    *
    * @return
    */
  def effectiveGrammar(entry: String): Grammar = {
    Grammar(effectiveModules(entry).toSeq)
  }

  /**
    * Get the modules that are part of the language.
    *
    * @param entry
    * @return
    */
  def effectiveModules(entry: String): Set[Module] = {
    dependenciesTransitive(getModule(entry))
  }

  /**
    * Get all directly imported modules for the given module.
    *
    * @param module
    * @return
    */
  def dependencies(module: Module): Set[Module] = {
    module
      .imports
      .map(getModule)
      .toSet
  }

  /**
    * Get all directly imported modules for the given modules.
    *
    * @param modules
    * @return
    */
  def dependencies(modules: Set[Module]): Set[Module] = {
    modules.flatMap(dependencies)
  }

  /**
    * Get the transitive closure of the import relation on the given module.
    *
    * @param modules
    * @return
    */
  def dependenciesTransitive(modules: Set[Module]): Set[Module] = {
    fixedPoint((modules: Set[Module]) => modules ++ dependencies(modules), modules)
  }

  /**
    * Get the transitive closure of the import relation on the given module.
    *
    * @param module
    * @return
    */
  def dependenciesTransitive(module: Module): Set[Module] = {
    dependenciesTransitive(Set(module))
  }
}
