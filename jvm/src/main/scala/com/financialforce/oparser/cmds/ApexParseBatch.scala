/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.oparser.cmds

import com.financialforce.oparser.{OutlineParser, TestClassFactory}

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicLong
import scala.annotation.unused

// Simple batch parser for benchmarking
@unused
object ApexParseBatch {

  @unused
  def main(paths: Array[String]): Unit = {
    println("Foo" + paths.mkString("Array(", ", ", ")"))
    if (paths.length < 1) {
      System.err.println(s"No paths provided to search for Apex classes")
      System.exit(1)
    }

    paths.tail.foreach(path => {
      val result = parseSeq(path)
      if (result != 0)
        System.exit(result)
    })
  }

  private def parseSeq(inPath: String): Int = {
    val start        = System.currentTimeMillis()
    val files        = scan(inPath)
    val timeForFiles = System.currentTimeMillis() - start

    files.foreach(f => {
      val status = parseFileWithStatus(f)
      if (status != 0)
        return status
    })

    println(s"Time to get file list ${timeForFiles / 1e3}s")
    println(s"Number of files: ${files.length}")
    println(s"Total Length: ${totalLength.longValue()} bytes")
    println(s"Read File Time: ${totalReadFileTime.longValue() / 1e3}s")
    println(s"Convert File Time: ${totalConvertFileTime.longValue() / 1e3}s")
    println(s"Parse Time: ${totalParseTime.longValue() / 1e3}s")
    println(s"Elapsed Time: ${(System.currentTimeMillis() - start) / 1e3}s")
    0
  }

  private val totalLength = new AtomicLong(0)

  private def addLength(l: Int): Unit = {
    totalLength.addAndGet(l)
  }

  private val totalParseTime = new AtomicLong(0)

  private def addParseTime(t: Long): Unit = {
    totalParseTime.addAndGet(t)
  }

  private val totalReadFileTime = new AtomicLong(0)

  private def addReadFileTime(t: Long): Unit = {
    totalReadFileTime.addAndGet(t)
  }

  private val totalConvertFileTime = new AtomicLong(0)

  private def addConvertFileTime(t: Long): Unit = {
    totalConvertFileTime.addAndGet(t)
  }

  private def parseFileWithStatus(file: Path): Int = {
    try {
      parseFile(file)
      0
    } catch {
      case ex: Throwable =>
        System.err.println(s"Failed parsing: $file")
        ex.printStackTrace()
        2
    }
  }

  private def parseFile(file: Path): Unit = {
    var start        = System.currentTimeMillis()
    val contentBytes = Files.readAllBytes(file)
    addLength(contentBytes.length)
    addReadFileTime(System.currentTimeMillis() - start)
    start = System.currentTimeMillis()
    val contentsString: String = new String(contentBytes, "utf8")
    addConvertFileTime(System.currentTimeMillis() - start)

    start = System.currentTimeMillis()
    val (success, reason, _) =
      OutlineParser.parse(file.toString, contentsString, TestClassFactory, null)
    if (!success) {
      if (reason.nonEmpty)
        println(s"outline-parser failed: $file ${reason.get}")
      else
        println(s"outline-parser failed: $file No reason given")
    }
    addParseTime(System.currentTimeMillis() - start)
  }

  private def scan(path: String): List[Path] = {
    val native = Paths.get(path)
    if (Files.isDirectory(native)) {
      scanDirectory(native)
        .filterNot(p => Files.isDirectory(p))
        .filter(_.toString.endsWith(".cls"))
    } else {
      List(Paths.get(path))
    }
  }

  private def scanDirectory(native: Path): List[Path] = {
    var files: List[Path] = Nil
    val paths             = Files.newDirectoryStream(native)
    try {
      paths.forEach(file => {
        val all: List[Path] = file :: (if (Files.isDirectory(file)) scanDirectory(file) else Nil)
        files = all ::: files
      })
      files
    } finally {
      paths.close()
    }
  }
}
