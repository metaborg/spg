package org.metaborg.spg.cmd.command

import org.backuity.clist._

class SentenceCommand extends Command(name = "sentence", description = "Generate random sentences") with CommonCommand {
  var ambiguity = opt[Boolean](
    description = "Test each generated sentence for ambiguity"
  )
}
