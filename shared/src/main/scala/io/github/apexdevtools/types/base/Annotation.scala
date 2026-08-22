/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.types.base

import scala.annotation.nowarn
import scala.collection.immutable.ArraySeq
import scala.util.hashing.MurmurHash3

/** Annotation element, name is case-insensitive.
  *
  * `parameterList` is the parameter list as written, see [[AnnotationParameter]]. It is empty when
  * the annotation was written without parentheses and an empty sequence when they were written but
  * hold nothing.
  *
  * `parameters` is the same content as a single unparsed string and is deprecated. It is a
  * concatenation of token contents, so it cannot represent the separator between two parameters,
  * which is the whole point of reading them.
  */
@nowarn("cat=deprecation") // this class necessarily still reads its own deprecated member
case class Annotation(
  name: String,
  @deprecated("Lossy, the separator between parameters is not recoverable, use parameterList")
  parameters: Option[String],
  location: Option[Location] = None,
  parameterList: Option[ArraySeq[AnnotationParameter]] = None
) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Annotation]
    name.equalsIgnoreCase(other.name) &&
    parameters.getOrElse("").equalsIgnoreCase(other.parameters.getOrElse("")) &&
    parameterList == other.parameterList
    // Note: location intentionally excluded from equality, see also AnnotationParameter which
    // excludes its own locations for the same reason.
  }

  override def hashCode(): Int = {
    MurmurHash3.orderedHash(
      Seq(name.toLowerCase(), parameters.getOrElse("").toLowerCase(), parameterList)
    )
    // Note: location intentionally excluded from hash. The parameters string is lowered here to
    // match the case-insensitive comparison in equals, previously it was not.
  }

  override def toString: String = {
    if (parameters.isDefined) s"@$name(${parameters.get})" else s"@$name"
  }
}

object Annotation {
  final val emptyArray = Array[Annotation]()
}
