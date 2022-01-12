package app.solver

import app.ScalaWrappers.{
  RichBody,
  RichChain,
  RichHost,
  RichLocal,
  RichSootMethod,
  SAssignStmt,
  SInstanceFieldRef,
  SInvokeExpr,
  SLocal,
  SNewExpr,
  SReturnStmt
}
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
    val declaringClassName = method.declaringClass.getName
    method.units.foldLeft((Set[Store](), Set[Load]())) { case (acc @ (stores, loads), ele) =>
      ele match {
        case SAssignStmt(SInstanceFieldRef(SLocal(receiver, _), field), SLocal(right, _)) if receiver == that.local =>
          (
            stores + (
              (
                VarField(VarPointer(method.name, receiver, declaringClassName), field.getName),
                VarPointer(method.getName, right, declaringClassName)
              )
            ),
            loads
          )
        case SAssignStmt(SLocal(left, _), SInstanceFieldRef(SLocal(receiver, _), field)) if receiver == that.local =>
          (
            stores,
            loads + (
              (
                VarPointer(method.name, left, declaringClassName),
                VarField(VarPointer(method.name, receiver, declaringClassName), field.getName)
              )
            )
          )
        case _ => acc
      }
    }
  }

  /** Make [[CallSite]] instance for matched invocation
    * @param returns maybe None for `foo.bar(...args)`
    * @param receiver `foo` in `[val ret = ]foo.bar(...args)`, maybe None for static-invoke
    * @param args `args` in `[val ret = ]` foo.bar(..args)`
    * @param abstracts virtually resolved method for current invocation
    * @param lineNumber lineNumber of callsite
    * @return
    */
  def mkCallSite(
      returns: Option[String], // return of calling site
      receiver: Option[Value], // receiver of calling site
      args: Seq[Value],        // args of calling site
      abstracts: SootMethod,   // abstract method to be dispatched
      lineNumber: Int,         // calling context line number
      that: VarPointer,        // specs receiver
      scope: SootMethod        // calling site method
  ) = {
    val caller = scope.declaringClass.getName
    receiver match {
      case Some(SLocal(name, _)) if (name == that.local) && (that.methodName == scope.name) && (that.clazz == caller) =>
        Some(
          CallSite(
            Some(that),
            abstracts,
            args.map { case SLocal(name, _) => VarPointer(scope.name, name, caller) },
            returns.map { local => VarPointer(scope.name, local, caller) },
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
        case SAssignStmt(SLocal(ret, _), SInvokeExpr(receiver, args, abstracts)) =>
          mkCallSite(Some(ret), receiver, args.toSeq, abstracts, ele.lineNumber, that, method)
        case SInvokeExpr(receiver, args, abstracts) =>
          mkCallSite(None, receiver, args.toSeq, abstracts, ele.lineNumber, that, method)
        case _ => None
      }).toSet ++ acc
    }

  /** Return all Return Var in [[method]]
    * @param method
    * @return
    */
  def returnOf(method: SootMethod): Set[VarPointer] = method.retrieveActiveBody().units.foldLeft(Set[VarPointer]()) {
    case (acc, SReturnStmt(SLocal((name, _)))) => acc + VarPointer(method.name, name, method.declaringClass.getName)
    case (acc, _)                              => acc
  }

  /** Return all Allocations Record for [[method]]
    * @param method
    * @return
    */
  def allocations(method: SootMethod): Set[(VarPointer, Allocation)] = method.units.foldLeft(Set[(VarPointer, Allocation)]()) { (acc, ele) =>
    acc ++ (ele match {
      case SAssignStmt(SLocal(allocated, _), SNewExpr(baseType)) =>
        Some(VarPointer(method.name, allocated, method.declaringClass.getName), Allocation(ele.lineNumber, baseType.toString))
      case _ => None
    }).toSet
  }

  /** Return all Assign Record for [[method]]
    * @param method
    * @return
    */
  def assigns(method: SootMethod): Set[(VarPointer, VarPointer)] = method.units.foldLeft(Set[(VarPointer, VarPointer)]()) { (acc, ele) =>
    acc ++ (ele match {
      case SAssignStmt(SLocal(assignee, _), SLocal(assigner, _)) =>
        Some(VarPointer(method.name, assignee, method.declaringClass.getName), VarPointer(method.name, assigner, method.declaringClass.getName))
      case _ => None
    }).toSet
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
    * when solver end (i.e worklist is empty), you can get indirect points-to relationship from [[pointerGraph]] or direct relationship via [[env]] and call-graph from [[callGraph]]
    */
  def solve() = {
    expand(entry)
    var current = worklist.removeHeadOption()
    while (current.nonEmpty) {
      val Some(pointer -> allocation) = current
      val delta                       = allocation -- env(pointer)
      propagate(pointer, mutable.Set.empty ++ delta)
      pointer match {
        case variable: VarPointer =>
          val (stores, loads) = reachableMethods.foldLeft((Set[Store](), Set[Load]())) { case (store -> load, it) =>
            val (s, l) = relatives(variable, it)
            (store ++ s, load ++ l)
          }
          delta.foreach { delta =>
            stores.foreach { case (variable, pointer) =>
              connect(pointer, FieldPointer(delta, variable.field))
            }
            loads.foreach { case (pointer, variable) =>
              connect(FieldPointer(delta, variable.field), pointer)
            }
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
    worklist ++= allocations(method).groupMap(_._1)(_._2).map { case key -> value => key -> (mutable.Set.empty ++ value) }
    assigns(method).foreach { case left -> right => connect(right, left) }
  }

  /** accumulate allocation info [[delta]] to [[pointer]], add all successor of [[pointer]] to worklist for pending process
    * @param pointer
    * @param delta
    */
  def propagate(pointer: Pointer, delta: mutable.Set[Allocation]) = if (delta.nonEmpty) {
    env.getOrElseUpdate(pointer, mutable.Set()).addAll(delta)
    for {
      node <- pointerGraph.find(pointer)
      next <- node.diSuccessors
    } {
      worklist += next.value -> (mutable.Set.empty ++ delta)
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
    reachableMethods.flatMap(it => invocations(it, receiver)).foreach { case callsite @ CallSite(_, abstracts, args, result, _) =>
      val target          = dispatch(self, abstracts)
      val targetClassName = target.declaringClass.getName
      target.body.thisLocal.foreach { case SLocal(receiverName, _) =>
        worklist += ((VarPointer(target.name, receiverName, targetClassName), mutable.Set(self)))
      }
      if (!callGraph.contains((callsite, target))) {
        callGraph.add((callsite, target))
        expand(target)
        args.zipWithIndex.foreach { case (arg, index) =>
          connect(arg, VarPointer(target.name, target.paramLocals(index).name, targetClassName))
        }
        for {
          to   <- result
          from <- returnOf(target)
        } {
          connect(from, to)
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
    Option(env(from)).filter(_.nonEmpty).foreach { it => worklist += ((to, it)) }
  }
}
