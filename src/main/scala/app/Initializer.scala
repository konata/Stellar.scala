package app

import soot.{G, Scene}
import soot.options.Options
import soot.util.ScalaWrappers.{RichOptions, RichSootClass}

import java.io.File
import scala.reflect.ClassTag

object Initializer {
  private val pattern = ".*pointsto.*jar$".r
  val excludes        = """soot.* java.* javax.* scala.*""".split(raw"""\s+""").filter(_.nonEmpty)
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
    val body  = clazz.methods.find(_.getName.contains(name)).head.retrieveActiveBody()
    body
  }
}
