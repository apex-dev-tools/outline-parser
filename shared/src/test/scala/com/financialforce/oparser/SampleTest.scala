/*
 * Copyright (c) 2023 Certinia Inc. All rights reserved.
 */
package com.financialforce.oparser

import com.financialforce.oparser.testutil.{AntlrOps, AntlrParser, ClassScanner}
import com.financialforce.types.ITypeDeclaration
import org.scalatest.Inspectors.forAll
import org.scalatest.funsuite.AnyFunSuite

class SampleTest extends AnyFunSuite {

  private val sampleDir = AntlrOps.samplesDir()
  assert(sampleDir.nonEmpty, "Set SAMPLES to location of apex-samples repo")

  forAll(ClassScanner.load(sampleDir.get)) { pathAndContent =>
    {
      val path    = pathAndContent._1
      val content = pathAndContent._2

      test("Parse " + path) {
        val (success, _, outlineDeclaration) =
          OutlineParser.parse(path, new String(content, "utf8"), TestClassFactory, null)

        var antlrDeclaration: Option[ITypeDeclaration] = None
        try {
          antlrDeclaration = AntlrParser.parse(path, content)
        } catch {
          case _: Exception => ()
        }

        assert(success == outlineDeclaration.nonEmpty)
        if (!success)
          assert(antlrDeclaration.isEmpty, "Outline parser failed when ANTLR parsed")
        else if (antlrDeclaration.nonEmpty) {
          outlineDeclaration.get match {
            case cls: TestClassTypeDeclaration =>
              Compare.compareClassTypeDeclarations(
                cls,
                antlrDeclaration.get.asInstanceOf[TestClassTypeDeclaration]
              )
            case int: TestInterfaceTypeDeclaration =>
              Compare.compareInterfaceTypeDeclarations(
                int,
                antlrDeclaration.get.asInstanceOf[TestInterfaceTypeDeclaration]
              )
            case enm: TestEnumTypeDeclaration =>
              Compare.compareEnumTypeDeclarations(
                enm,
                antlrDeclaration.get.asInstanceOf[TestEnumTypeDeclaration]
              )
            case _ =>
          }
        }
      }
    }
  }
}
