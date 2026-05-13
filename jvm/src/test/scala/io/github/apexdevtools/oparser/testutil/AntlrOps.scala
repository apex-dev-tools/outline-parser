/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */

package io.github.apexdevtools.oparser.testutil

import io.github.apexdevtools.types.base.Location
import org.antlr.v4.runtime.ParserRuleContext

import java.nio.file.{Path, Paths}

/* Additional ANTLR helpers for things not supported by apex-ls */
object AntlrOps {

  def samplesDir(): Option[Path] = {
    try {
      Option(System.getenv("SAMPLES"))
        .filter(_.nonEmpty)
        .map(Paths.get(_))
    } catch {
      case _: Throwable => None
    }
  }

  implicit class ContextOps(context: ParserRuleContext) {
    def location: Location = {
      // ANTLR uses [start, end) convention with exclusive end positions
      // We don't calculate byte offsets here as that would be too expensive
      // The comparison code will ignore byte offsets for ANTLR-generated locations
      Location(
        context.start.getLine,
        context.start.getCharPositionInLine,
        0, // byte offset not calculated
        context.stop.getLine,
        context.stop.getCharPositionInLine + context.stop.getText.length,
        0 // byte offset not calculated
      )
    }
  }
}
