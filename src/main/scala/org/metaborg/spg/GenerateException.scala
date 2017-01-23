package org.metaborg.spg

import org.metaborg.core.MetaborgException

case class GenerateException(s: String, e: MetaborgException) extends Exception(s, e)
