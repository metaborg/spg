package nl.tudelft.fragments.example

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import nl.tudelft.fragments.{Config, GeneratorEntryPoint}

object MiniJava {
  val config = new Config {
    override def sizeLimit: Int =
      60
  }

  def main(args: Array[String]): Unit = {
    new GeneratorEntryPoint().generate(
      sdfPath =
        "zip:/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.1.0.spoofax-language!/",
      nablPath =
        "zip:/Users/martijn/Projects/spoofax-releng/nabl/org.metaborg.meta.nabl2.lang/target/org.metaborg.meta.nabl2.lang-2.1.0.spoofax-language!/",
      projectPath =
        "/Users/martijn/Projects/MiniJava",
      semanticsPath =
        "trans/static-semantics.nabl2",
      limit =
        100,
      config =
        config
    ).subscribe(program => {
      println("===================================")
      println(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()))
      println("-----------------------------------")
      println(program)
    })
  }
}
