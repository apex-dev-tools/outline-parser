/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.oparser.cmds

import com.financialforce.oparser.{OutlineParser, TestClassFactory}
import io.scalajs.nodejs.fs.Fs
import io.scalajs.nodejs.path.Path
import io.scalajs.nodejs.process.Process

import java.util.concurrent.atomic.AtomicLong
import scala.annotation.unused
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

// Simple batch parser for benchmarking
@JSExportTopLevel("ApexParseBatch")
@unused
object ApexParseBatch {

  @JSExport
  @unused
  def main(paths: js.Array[String]): Unit = {
    if (paths.isEmpty) {
      System.err.println(s"No paths provided to search for Apex classes")
      Process.exit(1)
    }

    paths.foreach(path => {
      val result = parseSeq(path)
      if (result != 0)
        Process.exit(result)
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

  private def parseFileWithStatus(file: String): Int = {
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

  private def parseFile(file: String): Unit = {
    var start        = System.currentTimeMillis()
    val contentBytes = Fs.readFileSync(file).values().toIterator.map(_.toByte).toArray
    addLength(contentBytes.length)
    addReadFileTime(System.currentTimeMillis() - start)
    start = System.currentTimeMillis()
    val contentsString: String = new String(contentBytes, "utf8")
    addConvertFileTime(System.currentTimeMillis() - start)

    start = System.currentTimeMillis()
    val (success, reason, _) =
      OutlineParser.parse(file, contentsString, TestClassFactory, null)
    if (!success) {
      if (reason.nonEmpty)
        println(s"outline-parser failed: $file ${reason.get}")
      else
        println(s"outline-parser failed: $file No reason given")
    }
    addParseTime(System.currentTimeMillis() - start)
  }

  private def scan(path: String): Seq[String] = {
    val stat = Fs.statSync(path)

    if (stat.isDirectory()) {
      scanDirectory(path)
        .filterNot(p => Fs.statSync(p).isDirectory())
        .filter(_.endsWith(".cls"))
    } else {
      Seq(path)
    }
  }

  private def scanDirectory(path: String): Seq[String] = {
    Fs.readdirSync(path)
      .toSeq
      .flatMap(name => {
        val entry = Path.join(path, name)
        entry +: (if (Fs.statSync(entry).isDirectory()) scanDirectory(entry) else Nil)
      })
  }
}
