package org.metaborg.spg.core.spoofax.models

abstract class Character extends CharacterRange

case class Short(char: Char) extends Character {
  override def characters: Seq[Char] =
    List(char)
}
