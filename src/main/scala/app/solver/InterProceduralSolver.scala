package app.solver

import app.{Allocation, CallSite, Pointer, VarPointer}
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.mutable.Graph
import soot.SootMethod
import soot.jimple.Stmt
import soot.util.ScalaWrappers.RichSootMethod

import scala.collection.mutable

class InterProceduralSolver(entry: SootMethod) {
  val reachableMethods = mutable.Set[SootMethod]()
  val statements       = mutable.Set[Stmt]()
  val worklist         = mutable.Queue[(Pointer, mutable.Set[Allocation])]()
  val pointerGraph     = Graph[Pointer, DiEdge]()
  val callGraph        = mutable.Set[(CallSite, SootMethod)]()

  def solve(): Unit = {
    expand(entry)

  }
  def expand(method: SootMethod) = {
    if (!reachableMethods.contains(method)) {
      reachableMethods.add(method)

    }
  }
  def propagate(to: Pointer, allocations: Set[app.Allocation])   = ???
  def handleInvoke(receiver: VarPointer, allocation: Allocation) = ???
  def connect(to: Pointer, from: Pointer)                        = ???
  def dispatch(allocation: Allocation, method: SootMethod)       = ???

}
