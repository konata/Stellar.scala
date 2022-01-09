package app.solver

import app.{Allocation, CallSite, FieldPointer, Pointer, Receiver, Return, VarField, VarPointer}
import scalax.collection.GraphEdge.{DiEdge, ~>}
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.mutable.Graph
import soot.{Scene, SootClass, SootMethod}
import soot.jimple.Stmt
import soot.util.ScalaWrappers.{RichLocal, RichScene, RichSootMethod}

import scala.collection.mutable

class InterProceduralSolver(entry: SootMethod) {

  type Stores = Set[(VarField, Pointer)]
  type Loads  = Set[(VarPointer, VarField)]

  val reachableMethods = mutable.Set[SootMethod]()
  val statements       = mutable.Set[Stmt]()
  val worklist         = mutable.Queue[(Pointer, mutable.Set[Allocation])]()
  val pointerGraph     = Graph[Pointer, DiEdge]()
  val callGraph        = mutable.Set[(CallSite, SootMethod)]()
  val env              = mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())

  def relatives(receiver: VarPointer): (Stores, Loads) = ???
  def invocations(): Set[CallSite]                     = ???

  // Done@Solve
  def solve() {
    expand(entry)
    val current = worklist.removeHeadOption()
    while (current.nonEmpty) {
      val Some((pointer, allocation)) = current
      val delta                       = allocation -- env(pointer)
      propagate(pointer, delta.toSet)
      pointer match {
        case variable @ VarPointer(_, _) =>
          val (stores, loads) = relatives(variable)
          delta.foreach { delta =>
            // x.f = y
            stores.foreach { case (variable, pointer) => connect(FieldPointer(delta, variable.field), pointer) }
            // y = x.f
            loads.foreach { case (pointer, variable) => connect(pointer, FieldPointer(delta, variable.field)) }
          }
      }
    }
  }

  // Done@AddReachable
  def expand(method: SootMethod) = {
    if (!reachableMethods.contains(method)) {
      reachableMethods.add(method)
      worklist.addAll(method.allocations.map(it => (it._1, mutable.Set(it._2))))
      method.assigns.foreach { case (to, from) => connect(from, to) }
    }
  }

  def propagate(to: Pointer, allocations: Set[app.Allocation]) = ???
  def handleInvoke(receiver: VarPointer, self: Allocation): Unit = {
    invocations.foreach { case callsite @ CallSite(receiver, method, args, result, lineNumber) =>
      val target = dispatch(self, method)
      worklist += (Receiver(target), mutable.Set(self))
      if (!callGraph.contains((callsite, target))) {
        callGraph.add((callsite, target))
        reachableMethods += target
        args.zipWithIndex.foreach { case (arg, index) =>
          connect(VarPointer(target.name, target.paramLocals(index).name), arg)
        }
        connect(result, Return(target))
      }
    }
  }

  // AddEdge
  def connect(from: Pointer, to: Pointer) = if ((pointerGraph find from ~> to).isEmpty) {
    pointerGraph.add(from ~> to)
    worklist += ((to, env(from)))
  }

  def dispatch(allocation: Allocation, method: SootMethod): SootMethod = {
    val scene        = Scene.v()
    var clazz        = scene.getSootClass(allocation.clazz)
    val signature    = method.subSignature
    var targetMethod = clazz.getMethodUnsafe(signature)
    while (targetMethod == null) {
      clazz = clazz.getSuperclass
      targetMethod = clazz.getMethodUnsafe(signature)
    }
    targetMethod
  }

}
