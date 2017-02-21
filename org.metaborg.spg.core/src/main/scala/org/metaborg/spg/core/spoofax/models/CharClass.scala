package org.metaborg.spg.core.spoofax.models

abstract class CharClass extends Symbol {
  def characters: Seq[Char]
}

case class Simple(chars: CharacterRange*) extends CharClass {
  override def characters: Seq[Char] =
    chars.flatMap(_.characters)
}

case class Comp(charClass: CharClass) extends CharClass {
  override def characters: Seq[Char] =
    (33 to 126).map(_.toChar) diff charClass.characters
}
