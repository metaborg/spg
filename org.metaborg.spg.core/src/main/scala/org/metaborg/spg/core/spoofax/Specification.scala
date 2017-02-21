package org.metaborg.spg.core.spoofax

import org.metaborg.spg.core._
import org.metaborg.spg.core.regex._
import org.metaborg.spg.core.resolution.{Label, LabelOrdering}

/**
  * Representation of an NaBL2 specification.
  *
  * @param labels
  * @param order
  * @param wf
  * @param rules
  */
case class Specification(labels: List[Label], order: LabelOrdering, wf: Regex[Label], rules: List[Rule]) {
  /**
    * Merge this NaBL2 specification with the given NaBL2 specification.
    *
    * @param s
    */
  def merge(s: Specification): Specification = {
    Specification(labels ++ s.labels, order.merge(s.order), wf || s.wf, rules ++ s.rules)
  }
}
