package soot.util

import soot.tagkit._
import soot.util.ScalaWrappers._
import soot.{SootClass, Unit => SootUnit}

object AnnotationOps {

  def findAnnotation[A <: Tag](body: Host, clazz: Class[A]) = body.tagOpt(clazz)

  def hasJavaAnnotation(body: Host, annotation: String) = findJavaAnnotation(body, annotation).isDefined

  def hasJavaAnnotation(body: Host, annotations: IterableOnce[String]) = {
    val vat: Option[VisibilityAnnotationTag] = findAnnotation(body, classOf[VisibilityAnnotationTag])
    val javaAnnotationTypes = vat.toList.flatMap(_.annotations).map(_.getType).toSet
    annotations.iterator.exists(javaAnnotationTypes.contains)
  }

  def findJavaAnnotation(body: Host, annotation: String) = {
    val vat = findAnnotation(body, classOf[VisibilityAnnotationTag])
    vat.flatMap(_.annotations.find(_.getType == annotation))
  }

  def annotationElements(ann: AnnotationTag): Map[String, Any] = {
    ann.elements.collect {
      case elem: AnnotationStringElem => (elem.name, elem.getValue)
      case elem: AnnotationIntElem => (elem.name, elem.getValue)
      case elem: AnnotationBooleanElem => (elem.name, elem.getValue)
      case elem: AnnotationDoubleElem => (elem.name, elem.getValue)
      case elem: AnnotationFloatElem => (elem.name, elem.getValue)
      case elem: AnnotationLongElem => (elem.name, elem.getValue)
      case elem: AnnotationArrayElem => (elem.name, elem.getValues)
      case elem: AnnotationClassElem => (elem.name, elem.getDesc)
      case elem: AnnotationEnumElem => (elem.name, elem.getConstantName) //Do we need the type too?
      case elem: AnnotationAnnotationElem => (elem.name, elem.getValue)
    }.toMap
  }

  def elementsForJavaAnnotation(body: Host, annotation: String) = findJavaAnnotation(body, annotation).map(annotationElements).getOrElse(Map())

  def javaToSootForm(javaForm: String): String = {
    if (javaForm == null) throw new NullPointerException("Null annotation")
    if (javaForm.isEmpty) throw new IllegalArgumentException("Empty annotation name")
    if (javaForm.startsWith("L") && javaForm.endsWith(";")) throw new IllegalArgumentException("Annotation already in Soot form")
    "L" + javaForm.replace('.', '/') + ';'
  }

  def sourceFile(sc: SootClass): Option[String] = findAnnotation(sc, classOf[SourceFileTag]).map(_.getAbsolutePath)

}