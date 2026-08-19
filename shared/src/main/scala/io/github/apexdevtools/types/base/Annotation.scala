/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.types.base

import scala.collection.immutable.ArraySeq
import scala.util.hashing.MurmurHash3

/** Annotation element, name is case-insensitive.
  *
  * Parameters are available in two forms. `parameters` is the original unparsed string, a
  * concatenation of the token contents between the parentheses, empty when the annotation was
  * written without them. `parameterList` is the same content split into located parameters, see
  * [[AnnotationParameter]]; it is empty when the annotation was written without parentheses and an
  * empty sequence when they were written but hold nothing.
  */
case class Annotation(
  name: String,
  parameters: Option[String],
  location: Option[Location] = None,
  parameterList: Option[ArraySeq[AnnotationParameter]] = None
) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Annotation]
    name.equalsIgnoreCase(other.name) &&
    parameters.getOrElse("").equalsIgnoreCase(other.parameters.getOrElse(""))
    // Note: location & parameterList intentionally excluded from equality. Equality is over the
    // annotation as written, and annotations are compared across producers that populate only
    // 'parameters', so including parameterList would change set/map behaviour for existing
    // consumers. It would also be finer than 'parameters' alone, e.g. 'a=1 b=2' and 'a=1b=2' share
    // a parameters string but not a parameterList.
  }

  override def hashCode(): Int = {
    MurmurHash3.orderedHash(Seq(name.toLowerCase(), parameters.getOrElse("")))
    // Note: location & parameterList intentionally excluded from hash
  }

  override def toString: String = {
    if (parameters.isDefined) s"@$name(${parameters.get})" else s"@$name"
  }
}

object Annotation {
  final val emptyArray = Array[Annotation]()
}
