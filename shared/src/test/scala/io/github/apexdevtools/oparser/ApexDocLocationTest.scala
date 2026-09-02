/*
 * Copyright (c) 2026 Kevin Jones. All rights reserved.
 */
package io.github.apexdevtools.oparser

import io.github.apexdevtools.types._
import io.github.apexdevtools.types.base._
import org.scalatest.funspec.AnyFunSpec

import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

class ApexDocLocationTest extends AnyFunSpec {

  private def parse(contents: String): IMutableTestTypeDeclaration = {
    val (_, error, declaration) =
      OutlineParser.parse("Doc.cls", contents, TestClassFactory, ctx = null)
    error.foreach(message => throw new Exception(message))
    declaration.get
  }

  private def source(contents: String, location: Option[Location]): Option[String] = {
    val bytes = contents.getBytes(StandardCharsets.UTF_8)
    location.map(loc =>
      new String(bytes.slice(loc.startByteOffset, loc.endByteOffset), StandardCharsets.UTF_8)
    )
  }

  describe("type declarations") {
    it("captures exact UTF-8 locations for classes with formatting between the doc and type") {
      val contents =
        "/** café\r\n * class docs\r\n */\r\n\t@IsTest\r\npublic class Doc {}"
      val declaration = parse(contents)

      assert(source(contents, declaration.docLocation).contains("/** café\r\n * class docs\r\n */"))
      assert(declaration.docLocation.contains(Location(1, 0, 0, 3, 3, 29)))
    }

    it("captures docs for interfaces, enums, and inner types") {
      val interfaceContents = "/** interface */\ninterface Doc {}"
      val enumContents      = "/** enum */\nenum Doc {}"
      val innerContents =
        "class Doc {\n  /** inner */\n  interface Inner {}\n}"

      assert(
        source(interfaceContents, parse(interfaceContents).docLocation).contains("/** interface */")
      )
      assert(source(enumContents, parse(enumContents).docLocation).contains("/** enum */"))
      assert(
        source(innerContents, parse(innerContents).innerTypes.head.docLocation)
          .contains("/** inner */")
      )
    }
  }

  describe("body declarations") {
    it("captures docs for constructors, methods, properties, fields, and initializers") {
      val contents =
        """class Doc {
          |  /** constructor */
          |  Doc() {}
          |  /** method */
          |  void run() {}
          |  /** property */
          |  String name { get; set; }
          |  /** fields */
          |  Integer first, second;
          |  /** initializer */
          |  static {}
          |}
          |""".stripMargin
      val declaration = parse(contents)

      assert(
        source(contents, declaration.constructors.head.docLocation).contains("/** constructor */")
      )
      assert(source(contents, declaration.methods.head.docLocation).contains("/** method */"))
      assert(source(contents, declaration.properties.head.docLocation).contains("/** property */"))
      assert(
        declaration.fields.forall(field =>
          source(contents, field.docLocation).contains("/** fields */")
        )
      )
      assert(
        source(contents, declaration.initializers.head.docLocation).contains("/** initializer */")
      )
    }

    it("captures docs for interface methods and enum constants") {
      val interfaceContents =
        "interface Doc {\n  /** method */\n  void run();\n}"
      val enumContents =
        "enum Doc {\n  /** first */\n  First,\n  /** second */\n  Second\n}"

      val interfaceDeclaration = parse(interfaceContents)
      val enumDeclaration      = parse(enumContents)

      assert(
        source(interfaceContents, interfaceDeclaration.methods.head.docLocation)
          .contains("/** method */")
      )
      assert(
        enumDeclaration.fields.map(field => source(enumContents, field.docLocation)) ==
          ArraySeq(Some("/** first */"), Some("/** second */"))
      )
    }
  }

  describe("pending doc semantics") {
    it("allows whitespace but clears docs for intervening ordinary comments") {
      val contents =
        """/** type */
          |// not doc
          |class Doc {
          |  /** method */
          |  /* not doc */
          |  void run() {}
          |  /** field */
          |  // not doc
          |  Integer value;
          |}
          |""".stripMargin
      val declaration = parse(contents)

      assert(declaration.docLocation.isEmpty)
      assert(declaration.methods.head.docLocation.isEmpty)
      assert(declaration.fields.head.docLocation.isEmpty)
    }

    it("does not leak unclaimed docs or docs from method, initializer, and property bodies") {
      val contents =
        """class Doc {
          |  /** unclaimed */ ;
          |  void run() { /** method body */ }
          |  static { /** initializer body */ }
          |  String name { get { /** property body */ } }
          |  Integer after;
          |}
          |""".stripMargin
      val declaration = parse(contents)

      assert(declaration.methods.head.docLocation.isEmpty)
      assert(declaration.initializers.head.docLocation.isEmpty)
      assert(declaration.properties.head.docLocation.isEmpty)
      assert(declaration.fields.head.docLocation.isEmpty)
    }

    it("does not leak trailing docs from inner type bodies to sibling declarations") {
      val contents =
        """class Outer {
          |  class InnerClass { /** trailing class doc */ }
          |  void afterClass() {}
          |  interface InnerInterface { /** trailing interface doc */ }
          |  class AfterInterface {}
          |  enum InnerEnum { Value, /** trailing enum doc */ }
          |  void afterEnum() {}
          |  class Mid {
          |    class Leaf { /** trailing leaf doc */ }
          |    void midMethod() {}
          |  }
          |}
          |""".stripMargin
      val declaration    = parse(contents)
      val afterInterface = declaration.innerTypes.find(_.id.name == "AfterInterface").get
      val mid            = declaration.innerTypes.find(_.id.name == "Mid").get

      assert(declaration.methods.find(_.id.name == "afterClass").get.docLocation.isEmpty)
      assert(afterInterface.docLocation.isEmpty)
      assert(declaration.methods.find(_.id.name == "afterEnum").get.docLocation.isEmpty)
      assert(mid.methods.find(_.id.name == "midMethod").get.docLocation.isEmpty)
    }

    it("associates a doc written before annotations but drops one written after") {
      val contents =
        """class Doc {
          |  /** method */
          |  @AuraEnabled
          |  void run() {}
          |  /** field */
          |  @AuraEnabled
          |  Integer value;
          |  @AuraEnabled
          |  /** after annotations */
          |  void afterAnnotation() {}
          |}
          |""".stripMargin
      val declaration = parse(contents)

      assert(
        source(contents, declaration.methods.find(_.id.name == "run").get.docLocation)
          .contains("/** method */")
      )
      assert(source(contents, declaration.fields.head.docLocation).contains("/** field */"))
      assert(declaration.methods.find(_.id.name == "afterAnnotation").get.docLocation.isEmpty)
    }

    it("captures a doc abutting the declaration or holding a block comment opener") {
      val abutting = "/** doc */class Doc{}"
      val opener   = "/** a /* nested-ish */\nclass Doc {}"

      assert(source(abutting, parse(abutting).docLocation).contains("/** doc */"))
      assert(source(opener, parse(opener).docLocation).contains("/** a /* nested-ish */"))
    }

    it("uses only the last adjacent doc comment") {
      val contents = "/** old */\n/** current */\nclass Doc {}"
      assert(source(contents, parse(contents).docLocation).contains("/** current */"))
    }

    it("does not treat the empty block comment form as ApexDoc") {
      assert(parse("/**/ class Doc {}").docLocation.isEmpty)
      assert(
        source("/***/ class Doc {}", parse("/***/ class Doc {}").docLocation).contains("/***/")
      )
    }
  }

  describe("published API compatibility") {
    it("defaults read members and the mutable type writer for legacy implementations") {
      val body = new LegacyBodyDeclaration
      val tpe  = new LegacyMutableTypeDeclaration

      assert(body.docLocation.isEmpty)
      assert(tpe.docLocation.isEmpty)
      tpe.setDocLocation(Location.default)
      assert(tpe.docLocation.isEmpty)
    }

    it("keeps parseEnumMember reporting the ids of the members it appends") {
      val etd    = TestClassFactory.create("", ENUM_NATURE, "Doc.cls", None)
      val tokens = new Tokens
      tokens.append(LocatableIdToken("Value", Location.default))

      assert(Parse.parseEnumMember(etd, tokens).map(_.name) == Seq("Value"))
      assert(etd.fields.map(_.id.name) == ArraySeq("Value"))
    }
  }

  private class LegacyBodyDeclaration extends IBodyDeclaration {
    override def id: IdWithLocation              = LocatableIdToken("legacy", Location.default)
    override def bodyLocation: Option[Location]  = None
    override def blockLocation: Option[Location] = None
  }

  private class LegacyMutableTypeDeclaration extends IMutableTypeDeclaration {
    override def paths: Array[String]                = Array.empty
    override def location: Location                  = Location.default
    override def id: IdWithLocation                  = LocatableIdToken("Legacy", Location.default)
    override def typeNameSegment: TypeNameSegment    = TypeNameSegment(id, TypeRef.emptyArraySeq)
    override def enclosing: Option[ITypeDeclaration] = None
    override def extendsTypeRef: TypeRef             = null
    override def implementsTypeList: ArraySeq[TypeRef]                 = ArraySeq.empty
    override def modifiers: Array[Modifier]                            = Modifier.emptyArray
    override def annotations: Array[Annotation]                        = Annotation.emptyArray
    override def initializers: ArraySeq[IInitializer]                  = ArraySeq.empty
    override def innerTypes: ArraySeq[IMutableTypeDeclaration]         = ArraySeq.empty
    override def constructors: ArraySeq[IConstructorDeclaration]       = ArraySeq.empty
    override def methods: ArraySeq[IMethodDeclaration]                 = ArraySeq.empty
    override def properties: ArraySeq[IPropertyDeclaration]            = ArraySeq.empty
    override def fields: ArraySeq[IFieldDeclaration]                   = ArraySeq.empty
    override def setId(id: IdWithLocation): Unit                       = {}
    override def setLocation(location: Location): Unit                 = {}
    override def setExtends(typeRef: TypeRef): Unit                    = {}
    override def setImplements(typeList: ArraySeq[TypeRef]): Unit      = {}
    override def setModifiers(modifiers: Array[Modifier]): Unit        = {}
    override def setAnnotations(annotations: Array[Annotation]): Unit  = {}
    override def appendInitializer(init: Initializer): Unit            = {}
    override def appendInnerType(inner: IMutableTypeDeclaration): Unit = {}
    override def appendConstructor(ctor: ConstructorDeclaration): Unit = {}
    override def appendMethod(method: MethodDeclaration): Unit         = {}
    override def appendProperty(prop: PropertyDeclaration): Unit       = {}
    override def appendField(field: FieldDeclaration): Unit            = {}
    override def onComplete(): Unit                                    = {}
  }
}
