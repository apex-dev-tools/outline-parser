/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */
package com.financialforce.oparser.testutil

import com.nawforce.pkgforce.path.PathLike

object ClassScanner {

  def load(path: PathLike): Map[String, Array[Byte]] = {
    scan(path)
      .flatMap(file => {
        file.readBytes().map(c => (file.toString, c)).toOption
      })
      .toMap
  }

  def scan(path: PathLike): Seq[PathLike] = {
    if (path.isDirectory) {
      scanDirectory(path)
        .filterNot(_.isDirectory)
        .filter(_.toString.endsWith(".cls"))
    } else {
      Seq(path)
    }
  }

  private def scanDirectory(path: PathLike): Seq[PathLike] = {
    path
      .directoryList()
      .map(names => {
        names.flatMap(name => {
          val entry = path.join(name)
          entry +: (if (entry.isDirectory) scanDirectory(entry) else Nil)
        })
      })
      .getOrElse(Nil)
  }
}
