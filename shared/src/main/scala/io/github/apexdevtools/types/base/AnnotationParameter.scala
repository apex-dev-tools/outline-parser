/*
 * Copyright (c) 2026 Kevin Jones. All rights reserved.
 */
package io.github.apexdevtools.types.base

import scala.collection.immutable.ArraySeq

/** Separator written between two annotation parameters.
  *
  * Apex only accepts whitespace here; the platform compiler rejects a comma. Both forms are
  * recorded so that a consumer can diagnose the illegal one, the parser does not reject it.
  */
sealed abstract class AnnotationParameterSeparator(val text: String) {
  override def toString: String = text
}

object AnnotationParameterSeparator {

  /** Whitespace, the only separator the platform compiler accepts. */
  case object Whitespace extends AnnotationParameterSeparator(" ")

  /** A comma, which the platform compiler rejects. */
  case object Comma extends AnnotationParameterSeparator(",")
}

/** A single parameter of an annotation, as it was written.
  *
  * `name` is set only for the `name = value` form, it is empty for a bare value such as the
  * argument of `@SuppressWarnings('PMD')`. Values are left uninterpreted, quotes and all, no
  * attempt is made to establish whether the name or value is legal for the annotation.
  *
  * As with the unparsed `Annotation.parameters` string, the text of a value is a concatenation of
  * token contents so any whitespace inside it is not preserved. Use `valueLocation` against the
  * source if the exact text matters.
  *
  * `precedingSeparator` is the separator written between this parameter and the one before it, it
  * is empty for the first parameter of a list.
  */
final case class AnnotationParameter(
  name: Option[String],
  value: String,
  precedingSeparator: Option[AnnotationParameterSeparator] = None,
  nameLocation: Option[Location] = None,
  valueLocation: Option[Location] = None,
  location: Option[Location] = None
) {
  override def toString: String = name.map(n => s"$n=$value").getOrElse(value)
}

object AnnotationParameter {
  final val emptyArraySeq: ArraySeq[AnnotationParameter] = ArraySeq()
}
