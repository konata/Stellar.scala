package app.solver

import app.solver.InterProceduralSolver._
import app._
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.mutable.Graph
import soot.jimple.Stmt
import soot.util.ScalaWrappers.{
  RichBody,
  RichChain,
  RichHost,
  RichLocal,
  RichSootMethod,
  SAssignStmt,
  SInstanceFieldRef,
  SInvokeExpr,
  SLocal,
  SReturnStmt
}
import soot.{Scene, SootMethod, Value}

import scala.collection.mutable

object InterProceduralSolver {
  type Store  = (VarField, VarPointer)
  type Load   = (VarPointer, VarField)
  type Stores = Set[Store]
  type Loads  = Set[Load]

  def relatives(that: VarPointer, method: SootMethod) = {
    method.units.foldLeft((Set[Store](), Set[Load]())) { case (acc @ (stores, loads), ele) =>
      ele match {
        case SAssignStmt(SLocal(left, _), SInstanceFieldRef(SLocal(receiver, _), field)) if receiver == that.local =>
          (stores, loads + ((VarPointer(method.name, left), VarField(VarPointer(method.name, receiver), field.getName))))
        case SAssignStmt(SInstanceFieldRef(SLocal(receiver, _), field), SLocal(right, _)) if receiver == that.local =>
          (stores + ((VarField(VarPointer(method.name, receiver), field.getName), VarPointer(method.getName, right))), loads)
        case _ => acc
      }
    }
  }

  def mkCallSite(returns: Option[String], receiver: Option[Value], args: Seq[Value], method: SootMethod, lineNumber: Int) = CallSite(
    receiver.map { case SLocal(name, _) => VarPointer(method.name, name) },
    method,
    args.map { case SLocal(name, _) => VarPointer(method.name, name) },
    // TODO: fix case  `a.bar = a.foo()`
    returns.map { VarPointer(method.name, _) },
    lineNumber
  )

  def invocations(method: SootMethod): Set[CallSite] = method.retrieveActiveBody().units.foldLeft(Set[CallSite]()) { case (acc, ele) =>
    (ele match {
      // TODO:fix case `a.foo(b, *b.a* )`
      case SAssignStmt(SLocal(ret, _), SInvokeExpr(receiver, args, method)) =>
        Some(mkCallSite(Some(ret), receiver, args.toSeq, method, ele.lineNumber))
      case SInvokeExpr(receiver, args, method) =>
        Some(mkCallSite(None, receiver, args.toSeq, method, ele.lineNumber))
      case _ => None
    }).toSet ++ acc
  }

  def returnOf(method: SootMethod): Set[VarPointer] = method.retrieveActiveBody().units.foldLeft(Set[VarPointer]()) {
    case (acc, SReturnStmt(SLocal((name, _)))) => acc + VarPointer(method.name, name)
    case (acc, _)                              => acc
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

class InterProceduralSolver(entry: SootMethod) {
  val reachableMethods = mutable.Set[SootMethod]()
  val statements       = mutable.Set[Stmt]()
  val worklist         = mutable.Queue[(Pointer, mutable.Set[Allocation])]()
  val pointerGraph     = Graph[Pointer, DiEdge]()
  val callGraph        = mutable.Set[(CallSite, SootMethod)]()
  val env              = mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())

  // Done@Solve
  def solve() {
    expand(entry)
    val current = worklist.removeHeadOption()
    while (current.nonEmpty) {
      val Some((pointer, allocation)) = current
      val delta                       = allocation -- env(pointer)
      propagate(pointer, mutable.Set(delta.toSeq: _*))
      pointer match {
        case variable: VarPointer =>
          val (stores, loads) = reachableMethods.foldLeft((Set[Store](), Set[Load]())) { case ((store, load), it) =>
            val (s, l) = relatives(variable, it)
            (store ++ s, load ++ l)
          }
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
  def expand(method: SootMethod) = if (!reachableMethods.contains(method)) {
    reachableMethods.add(method)
    worklist.addAll(method.allocations.map { case (pointer, allocation) => (pointer, mutable.Set(allocation)) })
    method.assigns.foreach { case (to, from) => connect(from, to) }
  }

  def propagate(pointer: Pointer, delta: mutable.Set[Allocation]) = if (delta.nonEmpty) {
    env.getOrElseUpdate(pointer, mutable.Set()).addAll(delta)
    for (node <- pointerGraph.find(pointer)) {
      node.diSuccessors.foreach { node =>
        worklist += ((node.value, delta))
      }
    }
  }

  def handleInvoke(receiver: VarPointer, self: Allocation): Unit = {
    reachableMethods.flatMap(it => invocations(it)).foreach { case callsite @ CallSite(receiver, method, args, result, lineNumber) =>
      val target = dispatch(self, method)
      // TODO: workaround for receiver is null (static invoke)
      val SLocal(receiverName, _) = target.body.getThisLocal
      worklist += ((VarPointer(target.name, receiverName), mutable.Set(self)))
      if (!callGraph.contains((callsite, target))) {
        callGraph.add((callsite, target))
        reachableMethods += target
        args.zipWithIndex.foreach { case (arg, index) =>
          connect(VarPointer(target.name, target.paramLocals(index).name), arg)
        }
        result.foreach { to =>
          returnOf(target).foreach { from => connect(from, to) }
        }
      }
    }
  }

  def connect(from: Pointer, to: Pointer) = if ((pointerGraph find from ~> to).isEmpty) {
    pointerGraph.add(from ~> to)
    worklist += ((to, env(from)))
  }

}
