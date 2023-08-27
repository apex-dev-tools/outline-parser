/*
 * Copyright (c) 2021 FinancialForce.com, inc. All rights reserved.
 */
package com.financialforce.oparser.cmds

import com.financialforce.oparser._
import com.financialforce.oparser.testutil.{AntlrParser, ClassScanner}
import com.nawforce.pkgforce.path.PathLike
import com.nawforce.runtime.platform.Path

import java.util.concurrent.atomic.AtomicLong
import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

// Command for comparing outputs with apex-parser on one or many files
// NOTE: This needs to live in 'test' due to apex-ls dependency
object ApexParserCompare {
  def main(args: Array[String]): Unit = {
    System.exit(ApexParserCompare.run(args))
  }

  private def run(args: Array[String]): Int = {
    val options    = Set("-seq", "-notest", "-display", "-antlr")
    val sequential = args.contains("-seq")
    val test       = !args.contains("-notest") && !args.contains("-antlr")
    val display    = args.contains("-display")
    val onlyANTLR  = args.contains("-antlr")
    val paths      = args.filterNot(options.contains)

    if (paths.isEmpty) {
      System.err.println(s"No paths provided to search for Apex classes")
      return 1
    }

    paths.foreach(path => {
      val result =
        if (sequential)
          parseSeq(display, test, onlyANTLR, path)
        else
          parsePar(display, test, onlyANTLR, path)
      if (result != 0)
        return result
    })

    // All good
    0
  }

  private def parseSeq(display: Boolean, test: Boolean, onlyANTLR: Boolean, inPath: String): Int = {
    val absolutePath = Path(inPath)
    println("SEQUENTIAL " + absolutePath.toString)

    val start        = System.currentTimeMillis()
    val files        = ClassScanner.scan(absolutePath)
    val timeForFiles = System.currentTimeMillis() - start

    files.foreach(f => {
      val status = parseFileWithStatus(display, test, onlyANTLR, f)
      if (status != 0)
        return status
    })

    println(s"Time to get file list ${timeForFiles / 1e3}s")
    println(s"Number of files: ${files.length}")
    println(s"Total Length: ${totalLength.longValue()} bytes")
    println(s"Read File Time: ${totalReadFileTime.longValue() / 1e3}s")
    println(s"Convert File Time: ${totalConvertFileTime.longValue() / 1e3}s")
    println(s"Parse Time: ${totalParseTime.longValue() / 1e3}s")
    println(s"Antlr Time: ${totalAntlrTime.longValue() / 1e3}s")
    println(s"Elapsed Time: ${(System.currentTimeMillis() - start) / 1e3}s")
    0
  }

  private def parsePar(display: Boolean, test: Boolean, onlyANTLR: Boolean, inPath: String): Int = {
    val absolutePath = Path(inPath)
    println("PARALLEL " + absolutePath.toString)

    val start        = System.currentTimeMillis()
    val files        = ClassScanner.scan(absolutePath)
    val timeForFiles = System.currentTimeMillis() - start

    val all    = files.par.map(p => parseFileWithStatus(display, test, onlyANTLR, p))
    val result = if (all.exists(_ != 0)) 2 else 0

    println(s"Time to get file list ${timeForFiles / 1e3}s")
    println(s"Number of files: ${files.length}")
    println(s"Total Length: ${totalLength.longValue()} bytes")
    println(s"Read File Time: ${totalReadFileTime.longValue() / 1e3}s")
    println(s"Convert File Time: ${totalConvertFileTime.longValue() / 1e3}s")
    println(s"Parse Time: ${totalParseTime.longValue() / 1e3}s")
    println(s"Antlr Time: ${totalAntlrTime.longValue() / 1e3}s")
    println(s"Elapsed Time: ${(System.currentTimeMillis() - start) / 1e3}s")

    result
  }

  private val totalLength = new AtomicLong(0)

  private def addLength(l: Int): Unit = {
    totalLength.addAndGet(l)
  }

  private val totalParseTime = new AtomicLong(0)

  private def addParseTime(t: Long): Unit = {
    totalParseTime.addAndGet(t)
  }

  private val totalAntlrTime = new AtomicLong(0)

  private def addAntlrTime(t: Long): Unit = {
    totalAntlrTime.addAndGet(t)
  }

  private val totalReadFileTime = new AtomicLong(0)

  private def addReadFileTime(t: Long): Unit = {
    totalReadFileTime.addAndGet(t)
  }

  private val totalConvertFileTime = new AtomicLong(0)

  private def addConvertFileTime(t: Long): Unit = {
    totalConvertFileTime.addAndGet(t)
  }

  private def parseFileWithStatus(
    display: Boolean,
    test: Boolean,
    onlyANTLR: Boolean,
    file: PathLike
  ): Int = {
    try {
      parseFile(display, test, onlyANTLR, file)
      0
    } catch {
      case ex: Throwable =>
        System.err.println(s"Failed parsing: $file")
        ex.printStackTrace()
        2
    }
  }

  private def parseFile(
    display: Boolean,
    test: Boolean,
    onlyANTLR: Boolean,
    file: PathLike
  ): Unit = {
    var start         = System.currentTimeMillis()
    val contentsBytes = file.readBytes().getOrElse(Array())
    addReadFileTime(System.currentTimeMillis() - start)
    start = System.currentTimeMillis()
    val contentsString: String = new String(contentsBytes, "utf8")
    addConvertFileTime(System.currentTimeMillis() - start)

    val td = if (test || !onlyANTLR) {
      start = System.currentTimeMillis()
      val (success, reason, decl) =
        OutlineParser.parse(file.toString, contentsString, TestClassFactory, null)
      if (!success) {
        if (reason.nonEmpty)
          println(s"outline-parser failed: $file ${reason.get}")
        else
          println(s"outline-parser failed: $file No reason given")
      }
      addParseTime(System.currentTimeMillis() - start)

      if (display) {
        println("=====================")
        println(decl.get)
      }
      decl
    } else None

    val antlrType = if (test || onlyANTLR) {
      try {
        start = System.currentTimeMillis()
        val decl = AntlrParser.parse(file.toString, contentsBytes)
        addAntlrTime(System.currentTimeMillis() - start)
        decl
      } catch {
        case ex: Exception =>
          println(s"apex-parser failed: $file $ex")
          None
      }
    } else None

    if (test && td.nonEmpty && antlrType.nonEmpty) {
      td.get match {
        case cls: TestClassTypeDeclaration =>
          Compare.compareClassTypeDeclarations(
            cls,
            antlrType.get.asInstanceOf[TestClassTypeDeclaration]
          )
        case int: TestInterfaceTypeDeclaration =>
          Compare.compareInterfaceTypeDeclarations(
            int,
            antlrType.get.asInstanceOf[TestInterfaceTypeDeclaration]
          )
        case enm: TestEnumTypeDeclaration =>
          Compare.compareEnumTypeDeclarations(
            enm,
            antlrType.get.asInstanceOf[TestEnumTypeDeclaration]
          )
        case _ =>
      }
    }
    addLength(contentsBytes.length)
  }
}
