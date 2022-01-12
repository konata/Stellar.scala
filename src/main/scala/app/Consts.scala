package app

import java.io.File

object Consts {
  val pattern         = ".*pointsto.*jar$".r
  val excludes        = """soot.* java.* javax.* scala.*""".split(raw"""\s+""").filter(_.nonEmpty)
  val instrumentsPath = new File("target/scala-2.13/").listFiles.filter(pattern matches _.getName).map(_.getAbsolutePath)
}
