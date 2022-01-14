package app

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.io.dot.{DotAttr, DotEdgeStmt, DotNodeStmt, DotRootGraph, Id, NodeId, graph2DotExport}
import scalax.collection.mutable.Graph

import scala.collection.mutable
import Visualizer._

import java.io.{File, FileWriter}

case class Visualizer(val id: String) {
  val snapshots = mutable.Buffer[Snapshot]()

  private def makeSnapshot(
      worklist: mutable.Queue[(Pointer, mutable.Set[Allocation])],
      pointerGraph: Graph[Pointer, DiEdge],
      env: mutable.Map[Pointer, mutable.Set[Allocation]]
  ) = (worklist.toSeq.map { case key -> value => key -> value.toSet }, pointerGraph, env.map { case key -> value => key -> value.toSet }.toMap)

  def record(
      worklist: mutable.Queue[(Pointer, mutable.Set[Allocation])],
      pointerGraph: Graph[Pointer, DiEdge],
      env: mutable.Map[Pointer, mutable.Set[Allocation]]
  ) = snapshots.addOne(makeSnapshot(worklist, pointerGraph, env))

  def clear() = snapshots.clear()

  def dump() = snapshots.map { case (worklist, pointerGraph, env) =>
    val pixel = (1 to 6 ) ++ (1 to 6)


    def tips(pointer: Pointer) = env.get(pointer) match {
      case None              => Seq(DotAttr(Id("color"), Id("white")))
      case Some(allocations) => Seq(DotAttr(Id("color"), Id(s"\"#${pixel.drop(allocations.size - 1).take(6).mkString("")}\"")), tooltip(allocations.mkString))
    }

    val root = DotRootGraph(directed = true, Some(Id("PFG")))

    pointerGraph.toDot(
      root,
      it => Some((root, DotEdgeStmt(NodeId(it._1.toString()), NodeId(it._2.toString)))),
      cNodeTransformer = Some {
        _.value match {
          case vp: VarPointer   => Some(root, DotNodeStmt(NodeId(vp.toString), Seq(box) ++ tips(vp)))
          case fp: FieldPointer => Some(root, DotNodeStmt(NodeId(fp.toString), Seq(oval) ++ tips(fp)))
        }
      }
    )

  }
}

object Visualizer {
  type Snapshot         = (Seq[(Pointer, Set[Allocation])], Graph[Pointer, DiEdge], Map[Pointer, Set[Allocation]])
  type PointerFlowGraph = Graph[Pointer, DiEdge]

  val Array(yellow, cyan, red, _*) = "yellow cyan red".split("""\s+""").map(color => DotAttr(Id("color"), Id(color)))
  val Array(box, oval, circle, _*) = "box oval circle".split("""\s+""").map(shape => DotAttr(Id("shape"), Id(shape)))
  def tooltip(tips: String)        = DotAttr(Id("tooltip"), Id(tips))
}
