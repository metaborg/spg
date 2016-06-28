package nl.tudelft.fragments

import javax.inject.Singleton

import org.metaborg.core.project.{IProjectService, SimpleProjectService}
import org.metaborg.spoofax.core.{Spoofax, SpoofaxModule}

object Main {
  val spoofax = new Spoofax(new SpoofaxModule() {
    override def bindProject() {
      bind(classOf[SimpleProjectService]).in(classOf[Singleton])
      bind(classOf[IProjectService]).to(classOf[SimpleProjectService])
    }
  })
}

//object Main {
//  def main(args: Array[String]): Unit = {
////    val rules = Lambda.rules
////    val types = Lambda.types
////    val printer = Printer.print("/Users/martijn/Documents/workspace/Lamda")
//
//    val rules = MiniJava.rules
//    val types = MiniJava.types
//    val printer = Printer.printer("/Users/martijn/Documents/workspace/MiniJava")
//
////    val rules = Simple.rules
////    val types = Simple.types
////    val printer = (x: IStrategoTerm) => new StrategoString(x.toString(), null, 0)
//
//    // Start variable
//    val start = TermVar("x", SortAppl("Program"), TypeVar("t"), List(ScopeVar("s")))
//
//    // Graph that memorizes generation paths
//    val graph = Node(start)
//
//    // Make the generator repeat at most 10 times
//    for (i <- 1 to 7000) {
//      // Reset name provider so equals paths in the generation graph are actually equal
//      nameProvider.reset()
//
//      val r = Generator.generate(rules, Rule(
//        start,
//        SortAppl("Program"),
//        TypeVar("t"),
//        List(ScopeVar("s")),
//        State(Nil)
//      ), 25, graph, graph)
//
//      if (r.isDefined) {
//        val soln = Solver.solve(r.get._1.constraints)
//
//        if (soln.nonEmpty) {
////          println(r)
//
////          println(soln)
//
//          val concrete = Concretor.concretize(r.get._1, soln.get.constraints)
////          println(concrete)
//
//          val sterm = Converter.toTerm(concrete)
////          println(sterm)
//
//          val s = printer(sterm)
//          println(s.stringValue())
//
//          println("-----")
//        }
//      }
//    }
//  }
//}
