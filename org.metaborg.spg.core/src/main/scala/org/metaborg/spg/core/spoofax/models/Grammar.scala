package org.metaborg.spg.core.spoofax.models

/**
  * An SDF grammar consisting of two lists of context-free and lexical
  * productions.
  *
  * @param contextFree
  * @param lexical
  */
case class Grammar(contextFree: List[Production], lexical: List[Production]) {
  /**
    * Get both context-free and lexical productions.
    *
    * @return
    */
  def productions: List[Production] = {
    contextFree ++ lexical
  }

  /**
    * Derive the signature from the productions.
    *
    * @return
    */
  def toSignatures: List[Signature] = {
    contextFree.flatMap(_.toSignature)
  }

  /**
    * Merge two grammars.
    *
    * @param grammar
    * @return
    */
  def merge(grammar: Grammar): Grammar = {
    Grammar(grammar.contextFree ++ contextFree, grammar.lexical ++ lexical)
  }
}

object Grammar {
  def empty: Grammar = {
    Grammar(Nil, Nil)
  }
}
