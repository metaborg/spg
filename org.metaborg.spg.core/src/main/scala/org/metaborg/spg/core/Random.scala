package org.metaborg.spg.core

import scala.util.{Random => BaseRandom}

// A fixed-seed Random class
class Random extends BaseRandom {
  setSeed(0)
}

// Overwrite the default Random object to use our fixed-seed Random class
object Random extends Random
