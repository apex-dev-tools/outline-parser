/*
 * Copyright (c) 2023 Kevin Jones. All rights reserved.
 */
package com.financialforce.oparser

import com.financialforce.types.ITypeDeclaration
import com.financialforce.types.base.{Location, Modifier}
import org.scalatest.funspec.AnyFunSpec

import java.nio.charset.StandardCharsets

class SmokeTest extends AnyFunSpec {

  def parse(dummyContents: String): ITypeDeclaration = {
    val (_, ex, td) = OutlineParser.parse("Dummy.cls", dummyContents, TestClassFactory, ctx = null)
    ex.foreach(ex => throw new Exception(ex))
    td.get
  }

  def extractLocation(contents: String, location: Location): String = {
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    new String(
      bytes.slice(location.startByteOffset, location.endByteOffset + 1),
      StandardCharsets.UTF_8
    )
  }

  it("parses a empty class") {
    val content =
      """public class Dummy {
        |}
        |""".stripMargin
    val td = parse(content)

    assert(extractLocation(content, td.location) == content)
    assert(td.paths sameElements Array("Dummy.cls"))

    assert(td.id.name == "Dummy")
    assert(extractLocation(content, td.id.location) == "Dummy")

    assert(td.typeNameSegment.id == td.id)
    assert(td.typeNameSegment.typeArguments.isEmpty)
    assert(td.typeName sameElements Array(td.typeNameSegment))

    assert(td.enclosing.isEmpty)
    assert(td.extendsTypeRef == null)
    assert(td.implementsTypeList == null)

    assert(td.modifiers sameElements Array(Modifier("public")))
    assert(td.annotations.isEmpty)

    assert(td.initializers.isEmpty)
    assert(td.innerTypes.isEmpty)
    assert(td.constructors.isEmpty)
    assert(td.methods.isEmpty)
    assert(td.properties.isEmpty)
    assert(td.fields.isEmpty)
  }

  it("parses a method") {
    val content =
      """public class Dummy {
        |  public void func() {
        |  }
        |}
        |""".stripMargin
    val td = parse(content)

    assert(td.methods.length == 1)

    val method = td.methods.head
    assert(method.typeRef.exists(_.fullName == "void"))
    assert(method.formalParameters.isEmpty)
    assert(method.id.name == "func")
    assert(extractLocation(content, method.id.location) == "func")

    assert(method.bodyLocation.nonEmpty)
    method.bodyLocation.foreach(bl =>
      assert(extractLocation(content, bl) == "public void func() {\n  }\n")
    )
    assert(method.blockLocation.nonEmpty)
    method.blockLocation.foreach(bl => assert(extractLocation(content, bl) == "\n  }\n"))

    assert(method.annotations.isEmpty)
    assert(method.modifiers sameElements Array(Modifier("public")))
  }

  it("parses a method with non-ascii characters in comment") {
    // There are three non-ascii characters here next to each other
    // Facepalm \ud83e\udd26
    // Zero width joiner u200d
    // Male sign \u2642 with 'text' variation selector \ufe0f
    // In Java they are represented as 5 UTF-16 characters
    val content =
      """public class Dummy {
        |  public void func() {
        |  // A non-ascii 🤦‍♂️
        |  }
        |}
        |""".stripMargin
    val td = parse(content)

    assert(td.methods.length == 1)

    val method = td.methods.head
    assert(method.typeRef.exists(_.fullName == "void"))
    assert(method.formalParameters.isEmpty)
    assert(method.id.name == "func")
    assert(extractLocation(content, method.id.location) == "func")

    assert(method.bodyLocation.nonEmpty)
    method.bodyLocation.foreach(bl =>
      assert(extractLocation(content, bl) == "public void func() {\n  // A non-ascii 🤦‍♂️\n  }\n")
    )
    assert(method.blockLocation.nonEmpty)
    method.blockLocation.foreach(bl =>
      assert(extractLocation(content, bl) == "\n  // A non-ascii 🤦‍♂️\n  }\n")
    )

    assert(method.annotations.isEmpty)
    assert(method.modifiers sameElements Array(Modifier("public")))
  }

  it("parses a method with non-ascii characters in string literal") {
    val content =
      """public class Dummy {
      |  public void func() {
      |    String a='🤦‍♂️';
      |  }
      |}
      |""".stripMargin
    val td = parse(content)

    assert(td.methods.length == 1)

    val method = td.methods.head
    assert(method.typeRef.exists(_.fullName == "void"))
    assert(method.formalParameters.isEmpty)
    assert(method.id.name == "func")
    assert(extractLocation(content, method.id.location) == "func")

    assert(method.bodyLocation.nonEmpty)
    method.bodyLocation.foreach(bl =>
      assert(extractLocation(content, bl) == "public void func() {\n    String a='🤦‍♂️';\n  }\n")
    )
    assert(method.blockLocation.nonEmpty)
    method.blockLocation.foreach(bl =>
      assert(extractLocation(content, bl) == "\n    String a='🤦‍♂️';\n  }\n")
    )

    assert(method.annotations.isEmpty)
    assert(method.modifiers sameElements Array(Modifier("public")))
  }

  it("errors on constructor without body terminated with semi-colon") {
    val content =
      """public class Dummy {
        |  public Dummy();
        |}
        |""".stripMargin

    val thrown = intercept[Exception] {
      parse(content)
    }
    assert(thrown.getMessage == "Unrecognised method [2.3 -> 2.16] public Dummy ( )")
  }

  it("errors on constructor without body terminated with comment & semi-colon") {
    val content =
      """public class Dummy {
        |  public Dummy()
        |  /* A comment */
        |  ;
        |}
        |""".stripMargin

    val thrown = intercept[Exception] {
      parse(content)
    }
    assert(thrown.getMessage == "Unrecognised method [2.3 -> 2.16] public Dummy ( )")
  }


  it("errors on constructor without body terminated by class end") {
    val content =
      """public class Dummy {
        |  public Dummy()
        |}
        |""".stripMargin

    val thrown = intercept[Exception] {
      parse(content)
    }
    assert(thrown.getMessage == "Unexpected '}'")
  }

}
