/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */

package com.financialforce.oparser.testutil

import com.financialforce.types.base.Location
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
      Location(
        context.start.line,
        context.start.charPositionInLine,
        context.start.startIndex,
        context.stop.line,
        context.stop.charPositionInLine + context.stop.text.length,
        context.stop.stopIndex
      )
    }
  }
}
