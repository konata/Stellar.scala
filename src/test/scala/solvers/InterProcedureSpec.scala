package solvers

import app.{Allocation, Builder, Solver, VarPointer}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import soot.Local
import app.ScalaWrappers.{RichBody, RichChain, RichSootMethod, SAssignStmt, SInstanceFieldRef, SInvokeExpr, SLocal}
import playground.animals.{Animal, Bird, BlackCat, Cat, Dog, Husky, Sparrow}
import playground.events.Instrumented

import scala.collection.mutable

class InterProcedureSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  "returnsOf" should "find all possible return vars" in {
    val (method, body) = Builder.ofMethod[Instrumented]("foo")
    val returns        = Solver.returnOf(method)
    returns.size should be(3)
    raw"""\s+""".r.split("0 3 4").foreach { it =>
      returns.mkString.contains(it) should be(true)
    }
  }

  // foo.bar = c
  // c = foo.bar
  // foobar(foo.bar) ??
  // foo.bar = foobar(foo.bar) ??
  "relatives" should "filter all loads and stores statements " in {
    val (method, bodies) = Builder.ofMethod[Instrumented]("relatives")
    val local            = bodies.getParameterLocal(0)
    val tuple            = Solver.relatives(VarPointer(method.name, local.getName, method.declaringClass.getName), method)
    println(tuple)
  }

  // TODO:
  "allocations" should "filter all allocation pointer " in {}
  // TODO:
  "assigns" should "filter all assigns" in {}

  "invocations" should "find all invocation for static-invoke special-invoke and virtual-invoke" in {}

  "all classes" should "resolve hierarchy class" in {
    Builder
  }

  "dispatch" should "find the most accurate implementation for method" in {
    val (method, _) = Builder.ofMethod[Animal]("foo")
    Seq(
      classOf[Animal],
      classOf[Bird],
      classOf[BlackCat],
      classOf[Cat],
      classOf[Dog],
      classOf[Husky],
      classOf[Sparrow]
    ).zip(
      Seq(
        classOf[Animal],
        classOf[Animal],
        classOf[Animal],
        classOf[Animal],
        classOf[Dog],
        classOf[Dog],
        classOf[Sparrow]
      )
    ).foreach { case (src, expected) =>
      val name = Solver.dispatch(Allocation(0, src.getName), method).declaringClass.getName
      assert(
        name == expected.getName,
        s"failed for $src -> $expected actually: $name"
      )
    }
  }

  "dispatch for static method" should "find the exactly method" in {}

  "receiver var" should "be found" in {}

  "arguments pass" should "propagate correctly" in {}

  "`a.bar = b.foo()`" should " never exists" in {
    val (_, bodies) = Builder.ofMethod[Instrumented]("assignWithCall")
    bodies.units.foreach {
      case SAssignStmt(SInstanceFieldRef(_), SInvokeExpr(_)) => assert(false)
      case _                                                 => None
    }
  }

  "`this` var of static method " should "be null" in {
    val (_, bodies) = Builder.ofMethod[Instrumented]("entry")
    assert(bodies.thisLocal.isEmpty)
  }

  "`a.foo(b, b.a)`" should "never exists" in {
    val (_, bodies) = Builder.ofMethod[Instrumented]("assignWithCall")
    bodies.units.foreach {
      case SAssignStmt(_, SInvokeExpr(_, args, _)) =>
        args.foreach { arg =>
          assert(arg.isInstanceOf[Local])
        }
      case SInvokeExpr(_, args, _) =>
        args.foreach { arg =>
          assert(arg.isInstanceOf[Local])
        }
      case _ => None
    }
  }

  "-- operator" should "not change original value" in {
    val a = mutable.Set(1, 2, 3, 4, 5)
    val b = mutable.Set(3, 4, 5)
    val c = a -- b
    println(a)
    println(b)
    println(c)
  }

  override protected def beforeAll(): Unit = {
    Builder.setup()
  }

}
