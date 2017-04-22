package org.metaborg.spg.cmd.command

import org.backuity.clist._

class TermCommand extends Command(name = "term", description = "Generate random terms") with CommonCommand {
  var fuel = opt[Int](
    description = "Fuel provided to the backtracker (default: 400)",
    default = 400
  )
}
