package app

import app.Consts._
import soot.{G, Scene}
import soot.options.Options
import soot.util.ScalaWrappers.{RichOptions, RichScene, RichSootClass}

import scala.reflect.ClassTag

object Builder {

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

  def ofClass[T: ClassTag] = Scene.v().sootClassOpt(implicitly[ClassTag[T]].runtimeClass.getName)

  def ofBody[T: ClassTag](name: String) = {
    val Some(clazz) = ofClass[T]
    val method      = clazz.methods.find(_.getName.contains(name)).head
    val body        = method.retrieveActiveBody()
    (method, body)
  }
}
