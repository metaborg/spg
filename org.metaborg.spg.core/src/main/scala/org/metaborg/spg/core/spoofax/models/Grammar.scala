package org.metaborg.spg.core.spoofax.models

/**
  * An SDF grammar consisting of two lists of context-free and lexical
  * productions.
  *
  * @param contextFree
  * @param lexical
  * @param kernel
  */
case class Grammar(contextFree: List[Production], lexical: List[Production], kernel: List[Production]) {
  /**
    * Get both context-free and lexical productions.
    *
    * @return
    */
  lazy val productions: List[Production] = {
    contextFree ++ lexical ++ kernel
  }

  /**
    * Derive the signatures from the productions.
    *
    * Compared to SDFs default implementation, we distinguish between sorts
    * IterStar and Iter for non-empty and empty lists, respectively.
    *
    * @return
    */
  def toSignatures: List[Signature] = {
    def contextFreeToSignature(production: Production): Option[Signature] = {
      def rhsToConstType(rhs: Symbol): Option[ConstType] = rhs match {
        case x@SortAppl(_, _) =>
          Some(ConstType(x))
        case Iter(sort: Sort) =>
          Some(ConstType(SortAppl("Iter", List(sort))))
        case IterStar(sort: Sort) =>
          Some(ConstType(SortAppl("IterStar", List(sort))))
        case IterSep(sort: Sort, _) =>
          Some(ConstType(SortAppl("Iter", List(sort))))
        case IterStarSep(sort: Sort, _) =>
          Some(ConstType(SortAppl("IterStar", List(sort))))
        case Opt(sort: Sort) =>
          Some(ConstType(SortAppl("Option", List(sort))))
        case _ =>
          None
      }

      if (!production.isBracket && !production.isReject) {
        production.cons match {
          case Some(name) =>
            Some(OpDecl(name, FunType(production.rhs.flatMap(rhsToConstType), ConstType(production.sort))))
          case None =>
            Some(OpDeclInj(FunType(production.rhs.flatMap(rhsToConstType), ConstType(production.sort))))
        }
      } else {
        None
      }
    }

    def lexicalToSignature(production: Production): Option[Signature] = {
      if (!production.isReject) {
        Some(OpDeclInj(FunType(List(ConstType(SortAppl("String"))), ConstType(production.sort))))
      } else {
        None
      }
    }

    contextFree.flatMap(contextFreeToSignature) ++ kernel.flatMap(contextFreeToSignature) ++ lexical.flatMap(lexicalToSignature)
  }

  /**
    * Merge two grammars.
    *
    * @param grammar
    * @return
    */
  def merge(grammar: Grammar): Grammar = {
    Grammar(
      grammar.contextFree ++ contextFree,
      grammar.lexical ++ lexical,
      grammar.kernel ++ kernel
    )
  }
}

object Grammar {
  def empty: Grammar = {
    Grammar(Nil, Nil, Nil)
  }
}
