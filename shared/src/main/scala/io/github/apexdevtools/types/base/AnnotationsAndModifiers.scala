/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.types.base

/** Helper for elements supporting annotations & modifiers */
trait AnnotationsAndModifiers {
  def annotations: Array[Annotation]
  def modifiers: Array[Modifier]

  def annotationsAndModifiers: String = (annotations ++ modifiers).mkString(" ")
}
