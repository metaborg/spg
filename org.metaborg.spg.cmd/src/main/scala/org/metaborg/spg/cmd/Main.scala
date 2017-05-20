package org.metaborg.spg.cmd

import org.backuity.clist.Cli
import org.metaborg.spg.cmd.command.{SentenceCommand, TermCommand}
import org.metaborg.spg.cmd.handler.{SentenceHandler, TermHandler}
import org.metaborg.spoofax.core.Spoofax

object Main extends App {
  val spoofax = new Spoofax(new SPGModule)

  /**
    * Entry-point of the CLI.
    *
    * Dispatch based on the invoked command.
    */
  Cli.parse(args).withProgramName("spg").withCommands(new SentenceCommand, new TermCommand) match {
    case Some(sentenceCommand: SentenceCommand) =>
      new SentenceHandler(sentenceCommand)
    case Some(termCommand: TermCommand) =>
      new TermHandler(termCommand)
    case _ =>
      throw new IllegalArgumentException("Unknown command")
  }
}
