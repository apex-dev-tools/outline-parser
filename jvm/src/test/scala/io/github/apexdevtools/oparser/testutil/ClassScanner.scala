/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */
package io.github.apexdevtools.oparser.testutil

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters._

object ClassScanner {

  def load(path: Path): Map[String, Array[Byte]] = {
    scan(path).map(file => file.toString -> Files.readAllBytes(file)).toMap
  }

  def scan(path: Path): Seq[Path] = {
    if (Files.isDirectory(path)) {
      val stream = Files.walk(path)
      try {
        stream
          .iterator()
          .asScala
          .filter(p => Files.isRegularFile(p) && p.toString.endsWith(".cls"))
          .toVector
      } finally stream.close()
    } else {
      Seq(path)
    }
  }
}
