package app

import soot.{G, Scene}
import soot.options.Options
import soot.util.ScalaWrappers.{RichOptions, RichScene, RichSootClass}

import java.io.File
import scala.reflect.ClassTag

object Initializer {
  private val pattern = ".*pointsto.*jar$".r
  val excludes        = """soot.* java.* javax.* scala.*""".split(raw"""\s+""").filter(_.nonEmpty)
  val instrumentsPath = new File("target/scala-2.13/").listFiles.filter(pattern matches _.getName).map(_.getAbsolutePath)

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

  def classOf[T: ClassTag] = Scene.v().sootClassOpt(implicitly[ClassTag[T]].runtimeClass.getName)

  def bodyOf[T: ClassTag](name: String) = {
    val Some(clazz) = classOf[T]
    val method      = clazz.methods.find(_.getName.contains(name)).head
    val body        = method.retrieveActiveBody()
    (method, body)
  }
}
