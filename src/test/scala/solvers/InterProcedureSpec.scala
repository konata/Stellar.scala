package solvers

import app.{Allocation, Builder, VarPointer}
import app.solver.{InterProceduralSolver, IntraProceduralSolver}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import sample.ziwu.Instrumented
import playground._
import soot.Local
import soot.util.ScalaWrappers.{RichBody, RichChain, RichSootMethod, SAssignStmt, SInstanceFieldRef, SInvokeExpr, SLocal}

class InterProcedureSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  "returnsOf" should "find all possible return vars" in {
    val (method, body) = Builder.ofBody[Instrumented]("foo")
    val returns        = InterProceduralSolver.returnOf(method)
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
    val (method, bodies) = Builder.ofBody[Instrumented]("relatives")
    val local            = bodies.getParameterLocal(0)
    val tuple            = InterProceduralSolver.relatives(VarPointer(method.name, local.getName), method)
    println(tuple)
  }

  "invocations" should "find all invocation for static-invoke special-invoke and virtual-invoke" in {}

  "all classes" should "resolve hierarchy class" in {
    Builder
  }

  "dispatch" should "find the most accurate implementation for method" in {
    val (method, _) = Builder.ofBody[Animal]("foo")
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
      val name = InterProceduralSolver.dispatch(Allocation(0, src.getName), method).declaringClass.getName
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
    val (_, bodies) = Builder.ofBody[Instrumented]("assignWithCall")
    bodies.units.foreach {
      case SAssignStmt(SInstanceFieldRef(_), SInvokeExpr(_)) => assert(false)
      case _                                                 => None
    }
  }

  "`this` var of static method " should "be null" in {
    val (_, bodies) = Builder.ofBody[Instrumented]("entry")
    assert(bodies.thisLocal.isEmpty)
  }

  "`a.foo(b, b.a)`" should "never exists" in {
    val (_, bodies) = Builder.ofBody[Instrumented]("assignWithCall")
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

  override protected def beforeAll(): Unit = {
    Builder.initialize()
  }

}
