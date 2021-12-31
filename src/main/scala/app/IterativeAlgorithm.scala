//package app
//
//import scalax.collection.GraphEdge.{DiEdge, ~>}
//import scalax.collection.GraphPredef.{EdgeAssoc, edgeSetToOuter}
//import scalax.collection.io.dot.{DotAttr, DotAttrStmt, DotEdgeStmt, DotGraph, DotNodeStmt, DotRootGraph, Elem, Id, NodeId, graph2DotExport}
//import scalax.collection.mutable.Graph
//
//import scala.collection.mutable
//import scala.util.Random
//
//sealed trait Pointer
//case class VarPointer(name: String) extends Pointer {
//  override def toString = name
//}
//case class FieldPointer(field: String, allocation: Alloc) extends Pointer {
//  override def toString = s"$allocation.$field"
//}
//case class Alloc(loc: Int, fileName: String) {
//  override def toString = s"($fileName@$loc)"
//}
//
//object IterativeAlgorithm {
//  type PointerFlowGraph = Graph[Pointer, DiEdge]
//
//  def main(args: Array[String]): Unit = {
//    val allocs @ Seq(alloc1, alloc2, alloc3, alloc4) = (1 to 2).flatMap(line => List("foo", "bar").map(Alloc(line, _)))
//    val vars @ Seq(var1, var2, var3, var4)           = (1 to 4).map(it => VarPointer(s"var$it"))
//    val fields @ Seq(f1, f2, f3, f4, f5, f6, f7, f8) = List("f1", "f2").flatMap(f => allocs.map(FieldPointer(f, _)))
//    val nodes                                        = (vars ++ fields).map(it => (it, mutable.Set(Random.shuffle(allocs).take(1).head)))
//    val map                                          = mutable.Map[Pointer, mutable.Set[Alloc]]() ++ nodes.toMap
//    val keys                                         = map.keys.toSeq
//    val graph                                        = Graph[Pointer, DiEdge]()
//
//    graph.addAll(keys)
//    keys.sliding(2).foreach { case src +: target +: _ => graph.add(src ~> target) }
//
//    println(Choreographer.dump(graph))
//
//  }
//}
