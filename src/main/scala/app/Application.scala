package app

import org.ziwu.Instrumented
import soot.options.Options
import soot.util.ScalaWrappers._
import soot.{G, Scene, SootFieldRef, Value}

import java.io.File
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag


// key of allocation site


case class Allocation(line: Int, clazz: String) {
  override def toString = s"$line@$clazz"
}

sealed trait Points

case class Var(local: Value) extends Points {
  override def toString = local match {
    case SLocal(underline) => s"${underline._1}"
    case _ => super.toString
  }
}

case class FieldReference(alloc: Allocation, fieldRef: SootFieldRef) extends Points {
  override def toString = s"$alloc.${fieldRef.name()}"
}


object Application {
  private val pattern = ".*pointsto.*jar$".r
  val excludes = """soot.* java.* javax.* scala.*""".split(raw"""\s+""").filter(_.nonEmpty)
  val instrumentsPath = new File("./target/scala-2.13/").listFiles.filter(it => pattern.matches(it.getName)).map(_.getAbsolutePath)

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
    val env = mutable.Map[Points, mutable.Set[Allocation]]().withDefaultValue(mutable.Set[Allocation]())

    bodies.units.foreach {
      // [Alloc] foo = new Foo()
      case statement@SAssignStmt(allocated, SNewExpr(baseType)) =>
        val use = statement.getUseBoxes.asScala.head
        println("alloc", allocated, s"use:$use ${baseType.className}#${statement.lineNumber}")
        env.getOrElseUpdate(Var(allocated), mutable.Set()).add(
          Allocation(statement.lineNumber, baseType.getClassName)
        )

      // [Store] foo.bar = b
      case SAssignStmt(SInstanceFieldRef(left, field), right) =>
        println("store", left, field, right)
        val pending = env(Var(right))
        env(Var(left)).foreach(it => env.getOrElseUpdate(FieldReference(it, field.makeRef()), mutable.Set()).addAll(pending))

      // [Load]  b = foo.bar
      case SAssignStmt(left, SInstanceFieldRef(right, field)) =>
        val pending = env(Var(right)).flatMap(it => env(FieldReference(it, field.makeRef())))
        env.getOrElseUpdate(Var(left), mutable.Set()).addAll(pending)

      // [VanillaCall] bar.foo(foo)
      case SInvokeExpr(base, args, method) =>
        println("TODO: vanilla-call", base, method, args)

      // [Call]  b = a.foo()
      // [Static Call / Special Call / Virtual Call]
      case SAssignStmt(left, SInvokeExpr(base, args, method)) =>
        println("TODO: assign-call", left, base, method, args)

      // [Assign] b = a
      case SAssignStmt(left, right) =>
        println("assign", left, right)
        env.getOrElseUpdate(Var(left), mutable.Set()).addAll(env(Var(right)))

      case other => () // println("***", other, other.getClass)
    }

    println("*******")

    println(env.view.filterKeys {
      case Var(SLocal(v)) => !v._1.startsWith("$")
      case _ => true
    }.toList.map { case (k, v) => s"$k->$v" }.mkString("\n"))
  }
}
