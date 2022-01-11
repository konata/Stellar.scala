package app.solver

import app.ScalaWrappers.{RichBody, RichChain, RichHost, RichLocal, RichSootMethod, SAssignStmt, SInstanceFieldRef, SInvokeExpr, SLocal, SReturnStmt}
import app._
import app.solver.InterProceduralSolver._
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef.EdgeAssoc
import scalax.collection.mutable.Graph
import soot.{Scene, SootMethod, Value}

import scala.collection.mutable

object InterProceduralSolver {
  type Store  = (VarField, VarPointer)
  type Load   = (VarPointer, VarField)
  type Stores = Set[Store]
  type Loads  = Set[Load]

  /** Return all `load` and `store` operations related for [[that]] in method [[method]]
    * {{{
    *   that.foo = bar
    *   bar = that.foo
    * }}}
    * @param that
    * @param method
    * @return
    */
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

  /** Make [[CallSite]] instance for matched invocation
    * @param returns maybe None for `foo.bar(...args)`
    * @param receiver `foo` in `[val ret = ]foo.bar(...args)`, maybe None for static-invoke
    * @param args `args` in `[val ret = ]` foo.bar(..args)`
    * @param method virtually resolved method for current invocation
    * @param lineNumber lineNumber of callsite
    * @return
    */
  def mkCallSite(returns: Option[String], receiver: Option[Value], args: Seq[Value], method: SootMethod, lineNumber: Int, that: VarPointer) = {
    receiver match {
      case Some(SLocal(name, _)) if (name == that.local) && (that.methodName == method.name) =>
        Some(
          CallSite(
            Some(VarPointer(method.name, name)),
            method,
            args.map { case SLocal(name, _) => VarPointer(method.name, name) },
            returns.map { VarPointer(method.name, _) },
            lineNumber
          )
        )
      case _ => None
    }
  }

  /** Return all invocations in [[method]], including `that.foo(...args)` and `val foo = that.foo(...args)`
    * @param method
    * @return
    */
  def invocations(method: SootMethod, that: VarPointer): Set[CallSite] =
    method.retrieveActiveBody().units.foldLeft(Set[CallSite]()) { case (acc, ele) =>
      (ele match {
        case SAssignStmt(SLocal(ret, _), SInvokeExpr(receiver, args, method)) =>
          mkCallSite(Some(ret), receiver, args.toSeq, method, ele.lineNumber, that)
        case SInvokeExpr(receiver, args, method) =>
          mkCallSite(None, receiver, args.toSeq, method, ele.lineNumber, that)
        case _ => None
      }).toSet ++ acc
    }

  /** Return all Return Var in [[method]]
    * @param method
    * @return
    */
  def returnOf(method: SootMethod): Set[VarPointer] = method.retrieveActiveBody().units.foldLeft(Set[VarPointer]()) {
    case (acc, SReturnStmt(SLocal((name, _)))) => acc + VarPointer(method.name, name)
    case (acc, _)                              => acc
  }

  /** Dispatch [[method]] for object [[allocation]], return the  resolved method
    * @param allocation
    * @param method
    * @return
    */
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

  def apply(entry: SootMethod): InterProceduralSolver = new InterProceduralSolver(entry)
}

class InterProceduralSolver(entry: SootMethod) {
  val reachableMethods = mutable.Set[SootMethod]()
  val worklist         = mutable.Queue[(Pointer, mutable.Set[Allocation])]()
  val pointerGraph     = Graph[Pointer, DiEdge]()
  val callGraph        = mutable.Set[(CallSite, SootMethod)]()
  val env              = mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())

  /** solve PointsTo Analysis from [[entry]],
    * when solver end (a.k.a worklist is empty), you can get indirect points-to relationship from [[pointerGraph]] or direct relationship via [[env]] and call-graph from [[callGraph]]
    */
  def solve() = {
    expand(entry)
    var current = worklist.removeHeadOption()
    while (current.nonEmpty) {
      val Some((pointer, allocation)) = current
      val delta                       = (Set.empty ++ allocation) -- env(pointer)
      propagate(pointer, mutable.Set.empty ++= delta)
      pointer match {
        case variable: VarPointer =>
          val (stores, loads) = reachableMethods.foldLeft((Set[Store](), Set[Load]())) { case ((store, load), it) =>
            val (s, l) = relatives(variable, it)
            (store ++ s, load ++ l)
          }
          delta.foreach { delta =>
            stores.foreach { case (variable, pointer) => connect(FieldPointer(delta, variable.field), pointer) }
            loads.foreach { case (pointer, variable) => connect(pointer, FieldPointer(delta, variable.field)) }
            handleInvoke(variable, delta)
          }
        case _ => ()
      }
      current = worklist.removeHeadOption()
    }
  }

  /** set [[method]] as reachable and initialize basic points-to information from [[method]]
    * @param method
    */
  def expand(method: SootMethod) = if (!reachableMethods.contains(method)) {
    reachableMethods += method
    worklist ++= method.allocations.map { case (pointer, allocation) => (pointer, mutable.Set(allocation)) }
    method.assigns.foreach { case (to, from) => connect(from, to) }
  }

  /** accumulate allocation info [[delta]] to [[pointer]], add all successor of [[pointer]] to worklist for pending process
    * @param pointer
    * @param delta
    */
  def propagate(pointer: Pointer, delta: mutable.Set[Allocation]) = if (delta.nonEmpty) {
    env.getOrElseUpdate(pointer, mutable.Set()).addAll(delta)
    for (node <- pointerGraph.find(pointer)) {
      node.diSuccessors.foreach { node =>
        worklist += ((node.value, delta))
      }
    }
  }

  /** handle invocation (virtual-invoke / static-invoke) related to [[receiver]] for its allocation [[self]],
    * this procedure involves 3 steps:
    *  1. dispatch to the right method per allocation info [[self]]
    *  2. connect current allocation site with [[this]] in target method by adding to worklist
    *  3. extend call graph
    *  4. mark target method as reachable
    *  5. connect points to info for each arguments
    *  6. connect possible returns from [[target]] to callsite
    * @param receiver
    * @param self
    */
  def handleInvoke(receiver: VarPointer, self: Allocation): Unit = {
    reachableMethods.flatMap(it => invocations(it, receiver)).foreach { case callsite @ CallSite(_, method, args, result, _) =>
      val target = dispatch(self, method)
      target.body.thisLocal.foreach { case SLocal(receiverName, _) =>
        worklist += ((VarPointer(target.name, receiverName), mutable.Set(self)))
      }
      if (!callGraph.contains((callsite, target))) {
        callGraph.add((callsite, target))
        expand(target)
        args.zipWithIndex.foreach { case (arg, index) =>
          connect(VarPointer(target.name, target.paramLocals(index).name), arg)
        }
        result.foreach { to =>
          returnOf(target).foreach { from => connect(from, to) }
        }
      }
    }
  }

  /** derive the info that [[to]] pointer was a superset of [[from]]
    * @param from
    * @param to
    * @return
    */
  def connect(from: Pointer, to: Pointer) = if ((pointerGraph find from ~> to).isEmpty) {
    pointerGraph.add(from ~> to)
    worklist += ((to, env(from)))
  }
}
