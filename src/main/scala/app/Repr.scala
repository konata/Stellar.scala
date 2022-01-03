package app

case class Allocation(line: Int, clazz: String) {
  override def toString() = s"$line#$clazz"
}

sealed trait Pointer
case class VarPointer(methodName: String, local: String) extends Pointer {
  override def toString = s"${methodName}_$local"
}
case class FieldPointer(alloc: Allocation, fieldName: String) extends Pointer {
  override def toString = s"($alloc).${fieldName}" // (10@Foo).bar
}

case class InstanceMember(name: String, field: String)
