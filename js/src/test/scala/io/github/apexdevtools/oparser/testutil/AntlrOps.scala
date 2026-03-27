/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */

package io.github.apexdevtools.oparser.testutil

import io.github.apexdevtools.types.base.Location
import com.nawforce.pkgforce.path.PathLike
import com.nawforce.runtime.parsers.CodeParser.ParserRuleContext
import com.nawforce.runtime.platform.Path
import io.scalajs.nodejs.process.Process

import scala.scalajs.js

/* Additional ANTLR helpers for things not supported by apex-ls */
object AntlrOps {
  type AntlrCollection[T] = js.Array[T]

  def samplesDir(): Option[PathLike] = {
    try {
      Process
        .env("SAMPLES")
        .toOption
        .filter(_.nonEmpty)
        .map(Path(_))
    } catch {
      case _: Throwable => None
    }
  }

  implicit class ContextOps[X <: ParserRuleContext](context: X) {
    def location: Location = {
      // ANTLR uses [start, end) convention with exclusive end positions
      // We don't calculate byte offsets here as that would be too expensive
      // The comparison code will ignore byte offsets for ANTLR-generated locations

      // Handle UndefOr[Token] in ScalaJS - context.stop might be undefined
      val stopToken = context.stop.toOption.getOrElse(context.start)

      Location(
        context.start.line,
        context.start.charPositionInLine,
        0,  // byte offset not calculated
        stopToken.line,
        stopToken.charPositionInLine + stopToken.text.length,
        0   // byte offset not calculated
      )
    }
  }
}
