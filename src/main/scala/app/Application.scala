package app

import app.solver.IntraProceduralSolver
import org.ziwu.Instrumented

object Application {
  def main(args: Array[String]): Unit = {
    Initializer.initialize()
    IntraProceduralSolver[Instrumented]("entry").analysis()
  }
}
