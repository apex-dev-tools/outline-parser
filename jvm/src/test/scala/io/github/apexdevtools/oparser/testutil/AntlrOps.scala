/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */

package io.github.apexdevtools.oparser.testutil

import io.github.apexdevtools.types.base.Location
import com.nawforce.pkgforce.path.PathLike
import com.nawforce.runtime.parsers.CodeParser.ParserRuleContext
import com.nawforce.runtime.platform.Path

/* Additional ANTLR helpers for things not supported by apex-ls */
object AntlrOps {
  type AntlrCollection[T] = java.util.List[T]

  def samplesDir(): Option[PathLike] = {
    try {
      Option(System.getenv("SAMPLES"))
        .filter(_.nonEmpty)
        .map(Path(_))
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
        0,  // byte offset not calculated
        context.stop.getLine,
        context.stop.getCharPositionInLine + context.stop.getText.length,
        0   // byte offset not calculated
      )
    }
  }
}
