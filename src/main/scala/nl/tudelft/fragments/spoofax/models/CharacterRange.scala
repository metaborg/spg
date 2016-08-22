package nl.tudelft.fragments.spoofax.models

abstract class CharacterRange {
  def characters: Seq[Char]
}

case class Range(start: Character, end: Character) extends CharacterRange {
  override def characters: Seq[Char] = (start, end) match {
    case (Short(x), Short(y)) =>
      x to y
  }
}
