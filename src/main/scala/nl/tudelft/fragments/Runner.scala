package nl.tudelft.fragments

import java.io.FileReader

import com.google.common.collect.ImmutableList
import nl.tudelft.fragments.spoofax.Utils
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.{Observable, Subscriber}
import rx.observables.StringObservable._
import observable._

// Verify generated terms against expectations
object Runner {
  val s = Main.spoofax

  def main(args: Array[String]): Unit = {
    val langPath = "/Users/martijn/Projects/MiniJava"
    val termPath = "/tmp/terms.log"

    val langImpl = Utils.loadLanguage(langPath)
    val termObservable = toScalaObservable(from(new FileReader(termPath))).split("===".r)

    termObservable
      .count(_ => true)
      .subscribe(i => println(s"Tested $i terms"))
    
    termObservable.subscribe(
      onNext = (term: String) => {
        val inputUnit = s.unitService.inputUnit(term, langImpl, null)
        val parseResult = s.syntaxService.parse(inputUnit)

        if (!parseResult.valid() || !ImmutableList.copyOf(parseResult.messages()).isEmpty) {
          println("Could not parse:")
          println(term)
        }

        // TODO: Static analysis
        // spoofax.analysisService.analyze(parseResult, ???)

        // TODO: Operation semantics (type safety)
      },

      onError = (e: Throwable) =>
        throw e,

      onCompleted = () =>
        println(s"Done")
    )
  }
}

// Expectation: no parse errors
// Expectation: no static semantic errors
// Expectation: type sound
