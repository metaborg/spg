package org.metaborg.spg.cmd.command

import org.backuity.clist.{Command, arg, opt}

trait CommonCommand { this: Command =>
  var limit = opt[Int](
    description = "Number of " +
      "terms to generate (default: 1,000,000)",
    default = 1000000
  )

  var sizeLimit = opt[Int](
    description = "Maximum size of terms to generate (default: 60)",
    default = 60
  )

  var seed = opt[Option[Int]](
    description = "Seed for the random number generator (default: random)"
  )

  var sdfPath = arg[String](
    description = "Path to the SDF language implementation archive"
  )

  var nablPath = arg[String](
    description = "Path to the NaBL2 language implementation archive"
  )

  var projectPath = arg[String](
    description = "Path to the Spoofax project of the language to generate terms for"
  )
}
