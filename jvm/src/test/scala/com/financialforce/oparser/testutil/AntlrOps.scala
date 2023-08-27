/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */

package com.financialforce.oparser.testutil

import com.financialforce.types.base.Location
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
      Location(
        context.start.getLine,
        context.start.getCharPositionInLine,
        context.start.getStartIndex,
        context.stop.getLine,
        context.stop.getCharPositionInLine + context.stop.getText.length,
        context.stop.getStopIndex
      )
    }
  }
}
