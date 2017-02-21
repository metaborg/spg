package org.metaborg.spg.core

import org.metaborg.core.MetaborgException

case class GenerateException(s: String, e: MetaborgException) extends Exception(s, e)
