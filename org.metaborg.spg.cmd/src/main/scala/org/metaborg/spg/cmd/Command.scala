package org.metaborg.spg.cmd

import org.backuity.clist.{Command => BaseCommand, _}

class Command extends BaseCommand(name = "generator", description = "Generate random well-formed terms") {
  var limit = opt[Int](
    description = "Number of " +
      "terms to generate (default: 1,000,000)",
    default = 1000000
  )

  var fuel = opt[Int](
    description = "Fuel provided to the backtracker (default: 400)",
    default = 400
  )

  var sizeLimit = opt[Int](
    description = "Maximum size of terms to generate (default: 60)",
    default = 60
  )

  var consistency = opt[Boolean](
    description = "Whether or not to perform the consistency check (default: true)",
    default = true
  )

  var throwOnUnresolvable = opt[Boolean](
    description = "Whether or not to throw an exception when a reference can never be resolved (default: false)",
    default = false
  )

  var verbosity = opt[String](
    description = "Verbosity of the output as log level, i.e. ERROR, WARN, INFO, etc (default: ERROR)",
    default = "ERROR"
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
