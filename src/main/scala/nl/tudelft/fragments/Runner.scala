package nl.tudelft.fragments

import javax.inject.Singleton

import com.google.common.collect.ImmutableList
import nl.tudelft.fragments.spoofax.Utils
import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

// Verify generated terms against expectations
object Runner {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })

  def main(args: Array[String]) = {
    val langPath = "/Users/martijn/Projects/scopes-frames/L3"
    val text = io.Source.fromFile("/tmp/terms.log").getLines.mkString("\n")
    val terms = text.split("===")

    for (term <- terms) {
      val langImpl = Utils.loadLanguage(langPath)
      val inputUnit = spoofax.unitService.inputUnit(term, langImpl, null)

      // Parse
      val parseResult: ISpoofaxParseUnit = spoofax.syntaxService.parse(inputUnit)

      if (!parseResult.valid() || !ImmutableList.copyOf(parseResult.messages()).isEmpty) {
        println("Could not parse: " + term)
      } else {
        println("Parsed without errors")
      }

      // TODO: Static analysis
      // spoofax.analysisService.analyze(parseResult, ???)

      // TODO: Operation semantics (type safety)
    }

    println("Done")
  }
}

// Expectation: no parse errors
// Expectation: no static semantic errors
// Expectation: type sound
