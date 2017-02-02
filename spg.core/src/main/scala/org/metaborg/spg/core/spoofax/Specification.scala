package org.metaborg.spg.core.spoofax

import org.metaborg.spg.core._
import org.metaborg.spg.core.regex._
import org.metaborg.spg.core.resolution.{Label, LabelOrdering}

/**
  * Representation of an NaBL2 specification.
  *
  * @param params
  * @param rules
  */
case class Specification(params: ResolutionParams, rules: List[Rule])

/**
  * Representation of NaBL2 resolution parameters.
  *
  * @param labels
  * @param order
  * @param wf
  */
case class ResolutionParams(labels: List[Label], order: LabelOrdering, wf: Regex[Label])
