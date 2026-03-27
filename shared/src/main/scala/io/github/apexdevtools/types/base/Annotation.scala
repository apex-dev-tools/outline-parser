/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.types.base

import scala.util.hashing.MurmurHash3

/** Annotation element, name is case-insensitive, parameters are unparsed. */
case class Annotation(name: String, parameters: Option[String], location: Option[Location] = None) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Annotation]
    name.equalsIgnoreCase(other.name) &&
    parameters.getOrElse("").equalsIgnoreCase(other.parameters.getOrElse(""))
    // Note: location intentionally excluded from equality
  }

  override def hashCode(): Int = {
    MurmurHash3.orderedHash(Seq(name.toLowerCase(), parameters.getOrElse("")))
    // Note: location intentionally excluded from hash
  }

  override def toString: String = {
    if (parameters.isDefined) s"@$name(${parameters.get})" else s"@$name"
  }
}

object Annotation {
  final val emptyArray = Array[Annotation]()
}
