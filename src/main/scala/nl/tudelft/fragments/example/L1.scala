package nl.tudelft.fragments.example

import java.io.{File, FileOutputStream, PrintWriter}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{Config, GeneratorEntryPoint}

object L1 {
  val config = new Config {
    override def sizeLimit: Int =
      60
  }

  def main(args: Array[String]): Unit = {
    val writer = new PrintWriter(
      new FileOutputStream(new File("l1-2.log"), true)
    )

    new GeneratorEntryPoint().generate(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/scopes-frames/L1",
      semanticsPath =
        "trans/static-semantics.nabl2",
      config =
        config
    ).subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)

      writer.println("===================================")
      writer.println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      writer.println("-----------------------------------")
      writer.println(program)
    })
  }
}
