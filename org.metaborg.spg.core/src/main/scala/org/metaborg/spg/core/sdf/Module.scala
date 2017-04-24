package org.metaborg.spg.core.sdf

import org.metaborg.spg.core.stratego.{Constructor, Injection, Operation}

/**
  * Representation of an SDF3 module.
  *
  * @param name
  * @param imports
  * @param contextFree
  * @param lexical
  * @param kernel
  */
case class Module(name: String, imports: List[String], contextFree: List[Production], lexical: List[Production], kernel: List[Production]) {
  /**
    * Get all productions (context-free, lexical, and kernel).
    *
    * @return
    */
  def productions: List[Production] = {
    contextFree ++ lexical ++ kernel
  }

  /**
    * Derive constructors from the productions.
    *
    * This code is based on https://goo.gl/lsxDlN, but we distinguish between
    * sorts Iter and IterStar, which stand for for non-empty and empty lists,
    * respectively.
    *
    * @return
    */
  def toConstructors: List[Constructor] = {
    def contextFreeToConstructor(production: Production): Option[Constructor] = {
      def symbolToSort(rhs: Symbol): Option[Sort] = rhs match {
        case x@SortAppl(_, _) =>
          Some(x)
        case Iter(sort: Sort) =>
          Some(SortAppl("Iter", List(sort)))
        case IterStar(sort: Sort) =>
          Some(SortAppl("IterStar", List(sort)))
        case IterSep(sort: Sort, _) =>
          Some(SortAppl("Iter", List(sort)))
        case IterStarSep(sort: Sort, _) =>
          Some(SortAppl("IterStar", List(sort)))
        case Opt(sort: Sort) =>
          Some(SortAppl("Option", List(sort)))
        case _ =>
          None
      }

      if (!production.isBracket && !production.isReject) {
        production.cons match {
          case Some(name) =>
            Some(Operation(name, production.rhs.flatMap(symbolToSort), production.sort))
          case None =>
            Some(Injection(symbolToSort(production.rhs.head).get, production.sort))
        }
      } else {
        None
      }
    }

    def lexicalToConstructor(production: Production): Option[Injection] = {
      if (!production.isReject) {
        Some(Injection(SortAppl("String"), production.sort))
      } else {
        None
      }
    }

    List.concat(
      contextFree.flatMap(contextFreeToConstructor),
      kernel.flatMap(contextFreeToConstructor),
      lexical.flatMap(lexicalToConstructor)
    )
  }
}
