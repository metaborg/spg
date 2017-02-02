package org.metaborg.spg.cmd

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import ch.qos.logback.classic.{Level, Logger}
import org.backuity.clist.Cli
import org.metaborg.spg.core.{Config, GeneratorEntryPoint}
import org.metaborg.spoofax.core.Spoofax
import org.slf4j.LoggerFactory
import org.metaborg.spg.core.Generator

object Generator extends App {
  Cli.parse(args).withCommand(new Command)(options => {
    val spoofax = new Spoofax(new SPGModule)


    // TODO: Have Spoofax load nabl and templatelang
    // TODO: a) have Spoofax injector instantiate the Generator
    // TODO: b) have Spoofax injector

    val generator = spoofax.injector.getInstance(classOf[Generator])

    // Create Spoofax with custom child module
    // Use Spoofax's injector to get generator instance
    // Use generator instance to generate!

    LoggerFactory
      .getLogger("ROOT").asInstanceOf[Logger]
      .setLevel(Level.toLevel(options.verbosity))

    new generator.generate(
      options.sdfPath,
      options.nablPath,
      options.projectPath,
      options.semanticsPath,
      Config(
        options.limit,
        options.fuel,
        options.sizeLimit,
        options.consistency,
        options.throwOnUnresolvable
      )
    ).subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  })
}
