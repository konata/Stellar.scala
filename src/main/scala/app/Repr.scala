package app

import soot.SootMethod
import soot.util.ScalaWrappers.RichSootMethod

case class Allocation(line: Int, clazz: String) {
  override def toString() = s"$line#$clazz" // 18#Foo
}

sealed trait Pointer
case class VarPointer(methodName: String, local: String) extends Pointer {
  override def toString = s"${methodName}_$local" // foo_$stack1
}
case class FieldPointer(alloc: Allocation, fieldName: String) extends Pointer {
  override def toString = s"($alloc).$fieldName" // (10@Foo).bar
}

case class Receiver(method: SootMethod) extends Pointer {
  override def toString = s"this@${method.name}"
}

case class Return(method: SootMethod) extends Pointer {
  override def toString = s"return@${method.name}"
}

case class VarField(receiver: VarPointer, field: String)
case class CallSite(receiver: VarPointer, method: SootMethod, args: Seq[Pointer], returns: Pointer, lineNumber: Int)
