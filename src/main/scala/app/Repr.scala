package app

import soot.{SootFieldRef, Value}
import soot.util.ScalaWrappers.SLocal

case class Allocation(line: Int, clazz: String) {
  override def toString() = s"$line#$clazz"
}

sealed trait Pointer
case class VarPointer(local: Value) extends Pointer {
  override def toString = local match {
    case SLocal(underline) => s"${underline._1}"
    case _                 => ???
  }
}
case class FieldPointer(alloc: Allocation, fieldRef: SootFieldRef) extends Pointer {
  override def toString = s"($alloc).${fieldRef.name()}" // (10@Foo).bar
}
