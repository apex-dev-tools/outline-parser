/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.types.base

/** Modifier element, text is case-insensitive. */
case class Modifier(text: String, location: Option[Location] = None) {
  override def equals(obj: Any): Boolean = {
    val other = obj.asInstanceOf[Modifier]
    text.equalsIgnoreCase(other.text)
    // Note: location intentionally excluded from equality
  }

  override def hashCode(): Int = {
    text.toLowerCase.hashCode
    // Note: location intentionally excluded from hash
  }

  override def toString: String = text
}

object Modifier {
  final val emptyArray = Array[Modifier]()
}
