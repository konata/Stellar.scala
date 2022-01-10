package app

import scalax.collection.GraphEdge.DiEdge
import scalax.collection.io.dot.{DotAttr, DotEdgeStmt, DotNodeStmt, DotRootGraph, Id, NodeId, graph2DotExport}
import scalax.collection.mutable.Graph
import scala.collection.mutable

object Choreographer {

  type PointerFlowGraph = Graph[Pointer, DiEdge]

  val Array(yellow, cyan, red, _*) = "yellow cyan red".split("""\s+""").map(color => DotAttr(Id("color"), Id(color)))
  val Array(box, oval, circle, _*) = "box oval circle".split("""\s+""").map(shape => DotAttr(Id("shape"), Id(shape)))
  def tooltip(tips: String)        = DotAttr(Id("tooltip"), Id(tips))

  implicit val root = DotRootGraph(true, Some(Id("PFG")))

  def dump(graph: PointerFlowGraph, map: Map[Pointer, mutable.Set[Allocation]]) = {
    def tips(pointer: Pointer) = tooltip(map(pointer).mkString)
    val dot = graph.toDot(
      root,
      it => Some((root, DotEdgeStmt(NodeId(it._1.toString()), NodeId(it._2.toString)))),
      cNodeTransformer = Some {
        _.value match {
          case vp @ VarPointer(_, _)           => Some(root, DotNodeStmt(NodeId(vp.toString), List(cyan, box, tips(vp))))
          case fp @ FieldPointer(alloc, field) => Some(root, DotNodeStmt(NodeId(fp.toString), List(yellow, tips(fp))))
        }
      }
    )
    println(dot)
  }
}
