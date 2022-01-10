//package app
//
//import org.ziwu.Instrumented
//import soot.util.ScalaWrappers.{RichBody, RichChain, RichHost, SAssignStmt, SInstanceFieldRef, SInvokeExpr, SLocal, SNewExpr}
//
//import scala.collection.mutable
//
//object Playground {
//  def foo(args: Array[String]): Unit = {
//    Initializer.initialize()
//    val (method, bodies) = Initializer.bodyOf[Instrumented]("entry")
//    val env              = mutable.Map[Pointer, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())
//
//    bodies.units.foreach {
//      // [Alloc] foo = new Foo()
//      case statement @ SAssignStmt(allocated, SNewExpr(baseType)) =>
//        env
//          .getOrElseUpdate(VarPointer(allocated), mutable.Set())
//          .add(
//            Allocation(statement.lineNumber, baseType.getClassName)
//          )
//
//      // [Store] foo.bar = b
//      case SAssignStmt(SInstanceFieldRef(left, field), right) =>
//        val pending = env(VarPointer(right))
//        env(VarPointer(left)).foreach(it => env.getOrElseUpdate(FieldPointer(it, field.makeRef()), mutable.Set()).addAll(pending))
//
//      // [Load]  b = foo.bar
//      case SAssignStmt(left, SInstanceFieldRef(right, field)) =>
//        val pending = env(VarPointer(right)).flatMap(it => env(FieldPointer(it, field.makeRef())))
//        env.getOrElseUpdate(VarPointer(left), mutable.Set()).addAll(pending)
//
//      // [Assign] b = a
//      case SAssignStmt(left, right) =>
//        env.getOrElseUpdate(VarPointer(left), mutable.Set()).addAll(env(VarPointer(right)))
//
//      // [VanillaCall] bar.foo(foo)
//      case SInvokeExpr(base, args, method) => ???
//
//      // [Call]  b = a.foo()
//      // [Static Call / Special Call / Virtual Call]
//      case SAssignStmt(left, SInvokeExpr(base, args, method)) => ???
//
//      // ignored cases
//      case _ => () // println("***", other, other.getClass)
//    }
//
//    // strip out stack var
//    println(
//      env.view
//        .filterKeys {
//          case VarPointer(SLocal(v)) => !v._1.startsWith("$")
//          case _                     => true
//        }
//        .mkString("\n")
//    )
//  }
//
//}
