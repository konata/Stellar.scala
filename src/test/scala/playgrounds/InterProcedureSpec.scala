package playgrounds

import app.{Initializer, VarPointer}
import app.solver.InterProceduralSolver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.ziwu.Instrumented
import soot.util.ScalaWrappers.{RichBody, RichChain, RichSootMethod, SAssignStmt, SLocal}

class InterProcedureSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterAll {

  "returnsOf" should "find all possible return vars" in {
    val (method, body) = Initializer.bodyOf[Instrumented]("foo")
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
    val (method, bodies) = Initializer.bodyOf[Instrumented]("relatives")
    val local            = bodies.getParameterLocal(0)
    val tuple            = InterProceduralSolver.relatives(VarPointer(method.name, local.getName), method)
    println(tuple)
  }

  "invocations" should "find all invocation for static-invoke special-invoke and virtual-invoke" in {}

  "dispatch" should "find the most accurate implementation for method" in {}

  "receiver var" should "be found" in {}

  "arguments pass" should "propagate correctly" in {}

  override protected def beforeAll(): Unit = {
    Initializer.initialize()
  }
}
