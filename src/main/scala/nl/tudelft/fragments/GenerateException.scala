package nl.tudelft.fragments

import org.metaborg.core.MetaborgException

case class GenerateException(s: String, e: MetaborgException) extends Exception(s, e)
