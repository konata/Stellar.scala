package app

import app.solver.{InterProceduralSolver, IntraProceduralSolver}
import playground.samples.Instrumented

object Application {
  def main(args: Array[String]): Unit = {
    Builder.setup()
    val (method, _) = Builder.ofMethod[Instrumented]("entry")
    val solver      = InterProceduralSolver(method)
    solver.solve()
    println("inter solver")
    println(solver.env.mkString("\n"))

    val solver2 = IntraProceduralSolver[Instrumented]("entry")
    solver2.solve()
    println("intra solver")
    println(solver2.env.mkString("\n"))
  }
}
