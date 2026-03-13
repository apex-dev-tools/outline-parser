/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.types.base

import com.financialforce.types.ArrayInternCache

import scala.util.hashing.MurmurHash3

/** Annotation element, name is case-insensitive, parameters are unparsed. */
case class Annotation(name: String, parameters: Option[String]) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Annotation]
    name.equalsIgnoreCase(other.name) &&
    parameters.getOrElse("").equalsIgnoreCase(other.parameters.getOrElse(""))
  }

  override def hashCode(): Int = {
    MurmurHash3.orderedHash(Seq(name.toLowerCase(), parameters.getOrElse("")))
  }

  override def toString: String = {
    if (parameters.isDefined) s"@$name(${parameters.get})" else s"@$name"
  }
}

/** Caching support for Arrays of annotations. */
object Annotation {
  final val emptyArray = Array[Annotation]()

  private val cache = new ArrayInternCache[Annotation]()

  def intern(annotations: Array[Annotation]): Array[Annotation] = {
    cache.intern(annotations)
  }
}
