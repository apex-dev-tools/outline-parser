/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.types.base

import scala.collection.immutable.ArraySeq
import scala.util.hashing.MurmurHash3

/** Annotation element, name is case-insensitive.
  *
  * `parameterList` is the parameter list as written, see [[AnnotationParameter]]. It is empty when
  * the annotation was written without parentheses and an empty sequence when they were written but
  * hold nothing.
  */
case class Annotation(
  name: String,
  parameterList: Option[ArraySeq[AnnotationParameter]] = None,
  location: Option[Location] = None
) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Annotation]
    name.equalsIgnoreCase(other.name) &&
    parameterList == other.parameterList
    // Equality is over the annotation as written: its name, and the parameters in the order and
    // form they were written in. Names and values are compared case-insensitively by
    // AnnotationParameter, as Apex treats them.
    // Note: location intentionally excluded, as it is on Modifier.
  }

  override def hashCode(): Int = {
    MurmurHash3.orderedHash(Seq(name.toLowerCase(), parameterList))
    // Note: location intentionally excluded from hash
  }

  /** Render the annotation as written, including the separators. Whitespace is normalised to a
    * single space, so this is a faithful but not verbatim rendering of the source.
    */
  override def toString: String = {
    parameterList match {
      case None => s"@$name"
      case Some(parameters) =>
        val rendered = parameters
          .map(parameter =>
            parameter.precedingSeparator.map(_.text).getOrElse("") + parameter.toString
          )
          .mkString
        s"@$name($rendered)"
    }
  }
}

object Annotation {
  final val emptyArray = Array[Annotation]()
}
