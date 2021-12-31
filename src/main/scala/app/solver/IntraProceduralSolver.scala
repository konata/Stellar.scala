package app.solver

import app.Initializer
import soot.util.ScalaWrappers.{RichBody, RichChain, SAssignStmt, SLocal, SNewExpr}
import scala.reflect.ClassTag

case class IntraProceduralSolver[T: ClassTag](methodName: String) {

  def analysis() = {
    val (method, bodies) = Initializer.bodyOf[T](methodName)
    // find all locals from allocation sites
    val pointers = bodies.units.foldLeft(Set[String]()) { (acc, ele) =>
      acc ++ (ele match {
        case SAssignStmt(SLocal(allocated, _), SNewExpr(baseType)) => Some(allocated)
        case SAssignStmt(SLocal(left, _), _)                       => Some(left)
        case assign @ SAssignStmt(_) =>
          println(s"assign statement we dont care: $assign")
          None
        case others =>
          println(s"other statement we dont care: $others")
          None
      }).toSet
    }
    println(s"pointers: $pointers")
  }

}
