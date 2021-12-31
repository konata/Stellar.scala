package app.solver

import app.{Allocation, Initializer, Pointer, VarPointer}
import scalax.collection.GraphEdge.{DiEdge, ~>}
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.mutable.Graph
import soot.util.ScalaWrappers.{RichBody, RichChain, RichHost, SAssignStmt, SLocal, SNewExpr}

import scala.collection.mutable
import scala.reflect.ClassTag

case class IntraProceduralSolver[T: ClassTag](methodName: String) {
  type PointerFlowGraph            = Graph[Pointer, DiEdge]
  type MutablePointerAllocationMap = mutable.Map[Pointer, mutable.Set[Allocation]]

  val env         = mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())
  val graph       = Graph[Pointer, DiEdge]()
  val (_, bodies) = Initializer.bodyOf[T](methodName)
  val worklist    = mutable.Queue[(Pointer, mutable.Set[Allocation])]()

  def addEdge(from: Pointer, to: Pointer) = {
    if ((graph find from ~> to).isEmpty) {
      graph.add(from ~> to)
      worklist += (to, env(from))
    }
  }

  def propagate(pointer: Pointer, delta: mutable.Set[Allocation]) = ???

  def analysis() = {
    worklist.addAll(bodies.units.foldLeft(mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())) { (acc, ele) =>
      ele match {
        case SAssignStmt(SLocal(allocated, _), SNewExpr(baseType)) =>
          acc.getOrElseUpdate(VarPointer(allocated), mutable.Set()).add(Allocation(ele.lineNumber, baseType.toString))
        case _ => ()
      }
      acc
    })

    bodies.units.foreach {
      case SAssignStmt(SLocal(left, _), SLocal(right, _)) => addEdge(VarPointer(right), VarPointer(left))
      case _                                              => ()
    }

    var work = worklist.removeHeadOption()
    while (work.isDefined) {
      val Some((pointer, allocation)) = work
      val delta                       = allocation -- env(pointer)
      propagate(pointer, delta)
      ???
      work = worklist.removeHeadOption()
    }
  }

}
