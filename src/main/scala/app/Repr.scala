package app

import soot.SootMethod

case class Allocation(line: Int, clazz: String) {
  override def toString = s"$line#$clazz" // 18#Foo
}

sealed trait Pointer
case class VarPointer(methodName: String, local: String, clazz: String) extends Pointer {
  override def toString = s"${clazz.replaceAll(raw"^.*\.", "")}::$methodName.$local" // foo_$stack1
}
case class FieldPointer(alloc: Allocation, fieldName: String) extends Pointer {
  override def toString = s"($alloc).$fieldName" // (10@Foo).bar
}

case class VarField(receiver: VarPointer, field: String)
case class CallSite(receiver: Option[VarPointer], abstracts: SootMethod, args: Seq[Pointer], returns: Option[Pointer], lineNumber: Int)
