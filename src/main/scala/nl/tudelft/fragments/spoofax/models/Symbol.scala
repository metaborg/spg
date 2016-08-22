package nl.tudelft.fragments.spoofax.models

abstract class Symbol

// Sort usage
case class Sort(name: String) extends Symbol

// Literal string
case class Lit(text: String) extends Symbol

// Optional symbol
case class Opt(symbol: Symbol) extends Symbol

// Kleene plus
case class Iter(symbol: Symbol) extends Symbol

// Kleene star
case class IterStar(symbol: Symbol) extends Symbol

// Kleene plus with separator
case class IterSep(symbol: Symbol, separator: String) extends Symbol

// Kleene star with separator
case class IterStarSep(symbol: Symbol, separator: String) extends Symbol
