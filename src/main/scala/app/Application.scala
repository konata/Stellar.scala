package app

import app.solver.{InterProceduralSolver, IntraProceduralSolver}
import playground.samples.Instrumented

object Application {
  def main(args: Array[String]): Unit = {
    Builder.initialize()
//    IntraProceduralSolver[Instrumented]("entry").run()
    val (method, _) = Builder.ofBody[Instrumented]("entry")
    val solver      = InterProceduralSolver(method)
    solver.solve()
    println(solver.env)
  }
}
