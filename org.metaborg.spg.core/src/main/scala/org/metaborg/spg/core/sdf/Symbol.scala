package org.metaborg.spg.core.sdf

abstract class Symbol

// Literal string
case class Lit(text: String) extends Symbol

// Case insensitive literal string
case class CiLit(text: String) extends Symbol

// Optional symbol
case class Opt(symbol: Symbol) extends Symbol

// Alternative symbol
case class Alt(s1: Symbol, s2: Symbol) extends Symbol

// Sequence symbol
case class Sequence(s1: Symbol, s2: Symbol) extends Symbol

// Kleene plus
case class Iter(symbol: Symbol) extends Symbol

// Kleene star
case class IterStar(symbol: Symbol) extends Symbol

// Kleene plus with separator
case class IterSep(symbol: Symbol, separator: String) extends Symbol

// Kleene star with separator
case class IterStarSep(symbol: Symbol, separator: String) extends Symbol

// Layout
case class Layout() extends Symbol
