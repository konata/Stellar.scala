package app

import org.ziwu.{Events, Instrumented}
import soot.jimple.{AssignStmt, InstanceFieldRef, NewExpr}
import soot.jimple.internal.JAssignStmt
import soot.{G, RefType, Scene}
import soot.options.Options
import soot.util.ScalaWrappers._

import java.net.URLClassLoader
import scala.reflect.ClassTag

object Application {
  private val pattern = ".*pointsto.*jar$"
  val excludes = """soot.* java.* javax.* scala.*""".split(raw"""\s+""").filter(_.nonEmpty)
  val instrumentsPath = classOf[Events].getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).filter(_.matches(pattern))

  def initialize() = {
    G.reset()
    val options = Options.v()
    options.allowPhantomRefs = true
    options.wholeProgram = true
    options.processPath = instrumentsPath
    options.excludes = excludes
    options.keepLineNumber = true
    options.setPhaseOption("jb", "use-original-names:true")
    Scene.v().loadNecessaryClasses()
  }

  def bodyOf[T: ClassTag](name: String) = {
    val clazz = Scene.v().getSootClass(implicitly[ClassTag[T]].runtimeClass.getName)
    val body = clazz.methods.find(_.name.contains(name)).head.retrieveActiveBody()
    body
  }

  def main(args: Array[String]): Unit = {
    initialize()
    val bodies = bodyOf[Instrumented]("entry")
    bodies.units.foreach {
      // [Alloc] foo = new Foo()
      case SAssignStmt(allocated, SNewExpr(baseType)) => println("alloc", allocated, baseType.className)
      // [Load]  b = foo.bar
      case SAssignStmt(left, SInstanceFieldRef(right, field)) => println("load", left, right, field)
      // [Store] foo.bar = b
      case SAssignStmt(SInstanceFieldRef(left, field), right) => println("store", left, field, right)
      // [Call]  b = a.foo()
      // [Static Call / Special Call / Virtual Call]
      case SAssignStmt(left, SInvokeExpr(base, args, method)) => println("assign-call", left, base, method, args)
      // [Assign] b = a
      case SAssignStmt(left, right) => println("assign", left, right)
      /*
      // [Vanilla Call] bar.foo(foo)
      case SInvokeExpr(base, args, method) => println("vanilla-call", base, method, args)
      */
      case other => println("***", other, other.getClass)
    }
  }
}
