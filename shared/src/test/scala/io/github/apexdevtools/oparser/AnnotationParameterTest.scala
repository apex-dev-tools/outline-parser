/*
 * Copyright (c) 2026 Kevin Jones. All rights reserved.
 */
package io.github.apexdevtools.oparser

import io.github.apexdevtools.types.base.AnnotationParameterSeparator.{Comma, Whitespace}
import io.github.apexdevtools.types.base.{Annotation, AnnotationParameter, Location}
import org.scalatest.funspec.AnyFunSpec

import scala.annotation.nowarn

import java.nio.charset.StandardCharsets

// The suite deliberately pins the behaviour of the deprecated 'parameters' string
@nowarn("cat=deprecation")
class AnnotationParameterTest extends AnyFunSpec {

  private var source: String = ""

  /* Parse an annotation written on a class, retaining the source for location checks. */
  def annotation(text: String): Annotation = {
    source = s"$text\npublic class Dummy {\n}\n"
    val (_, ex, td) = OutlineParser.parse("Dummy.cls", source, TestClassFactory, ctx = null)
    ex.foreach(ex => throw new Exception(ex))
    val annotations = td.get.annotations
    assert(annotations.length == 1)
    annotations.head
  }

  def parameters(text: String): Seq[AnnotationParameter] =
    annotation(text).parameterList.getOrElse(fail("no parameter list"))

  def extract(location: Option[Location]): String = {
    val bytes = source.getBytes(StandardCharsets.UTF_8)
    location
      .map(l => new String(bytes.slice(l.startByteOffset, l.endByteOffset), StandardCharsets.UTF_8))
      .getOrElse("<none>")
  }

  describe("presence of a parameter list") {
    it("has none when written without parentheses") {
      val a = annotation("@AuraEnabled")
      assert(a.parameters.isEmpty)
      assert(a.parameterList.isEmpty)
    }

    it("has an empty one when written with empty parentheses") {
      val a = annotation("@AuraEnabled()")
      assert(a.parameters.contains(""))
      assert(a.parameterList.exists(_.isEmpty))
    }
  }

  describe("parameter forms") {
    it("reads a single unnamed value") {
      val params = parameters("@SuppressWarnings('PMD')")
      assert(params.length == 1)
      assert(params.head.name.isEmpty)
      assert(params.head.value == "'PMD'")
      assert(params.head.precedingSeparator.isEmpty)
    }

    it("reads a named value") {
      val params = parameters("@AuraEnabled(cacheable=true)")
      assert(params.length == 1)
      assert(params.head.name.contains("cacheable"))
      assert(params.head.value == "true")
    }

    it("keeps a named value when spaced around the assignment") {
      val params = parameters("@AuraEnabled( cacheable = true )")
      assert(params.length == 1)
      assert(params.head.name.contains("cacheable"))
      assert(params.head.value == "true")
    }

    it("keeps separators inside a quoted value") {
      val params = parameters("@SuppressWarnings('PMD,Unused')")
      assert(params.length == 1)
      assert(params.head.name.isEmpty)
      assert(params.head.value == "'PMD,Unused'")
    }

    it("keeps whitespace inside a quoted value") {
      val params = parameters("@InvocableMethod(label='a b' description='c')")
      assert(params.map(_.value) == Seq("'a b'", "'c'"))
      assert(params.map(_.name) == Seq(Some("label"), Some("description")))
    }
  }

  describe("separators") {
    it("records whitespace between parameters") {
      val params = parameters("@AuraEnabled(cacheable=true scope='global')")
      assert(params.map(_.name) == Seq(Some("cacheable"), Some("scope")))
      assert(params.map(_.value) == Seq("true", "'global'"))
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Whitespace)))
    }

    it("records a comma between parameters, it is not legal Apex but must be visible") {
      val params = parameters("@AuraEnabled(cacheable=true, scope='global')")
      assert(params.map(_.name) == Seq(Some("cacheable"), Some("scope")))
      assert(params.map(_.value) == Seq("true", "'global'"))
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Comma)))
    }

    it("records a comma written without surrounding space") {
      val params = parameters("@AuraEnabled(cacheable=true,scope='global')")
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Comma)))
    }

    it("records a trailing comma as an empty parameter") {
      val params = parameters("@AuraEnabled(cacheable=true,)")
      assert(params.length == 2)
      assert(params(1).name.isEmpty)
      assert(params(1).value.isEmpty)
      assert(params(1).precedingSeparator.contains(Comma))
    }

    it("separates across a newline") {
      val params = parameters("@AuraEnabled(cacheable=true\n    scope='global')")
      assert(params.map(_.name) == Seq(Some("cacheable"), Some("scope")))
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Whitespace)))
    }

    it("does not separate where no separator was written") {
      // Not legal Apex, but the parser records what is there rather than rejecting it
      val params = parameters("@AuraEnabled(cacheable=truescope='global')")
      assert(params.length == 1)
      assert(params.head.name.contains("cacheable"))
      assert(params.head.value == "truescope='global'")
    }

    it("mixes separators") {
      val params = parameters("@InvocableMethod(label='x', description='y' category='z')")
      assert(params.map(_.name) == Seq(Some("label"), Some("description"), Some("category")))
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Comma), Some(Whitespace)))
    }
  }

  describe("nesting") {
    it("does not separate on a comma inside nested parentheses") {
      val params = parameters("@Dummy(a=f(1, 2) b=3)")
      assert(params.length == 2)
      assert(params.map(_.name) == Seq(Some("a"), Some("b")))
      assert(params.map(_.value) == Seq("f(1,2)", "3"))
      assert(params.map(_.precedingSeparator) == Seq(None, Some(Whitespace)))
    }

    it("does not start a parameter inside nested parentheses") {
      val params = parameters("@Dummy(a=f(b=1) c=2)")
      assert(params.map(_.name) == Seq(Some("a"), Some("c")))
      assert(params.map(_.value) == Seq("f(b=1)", "2"))
    }
  }

  describe("locations") {
    it("anchors the name, the value and the whole parameter") {
      val params = parameters("@AuraEnabled(cacheable=true scope='global')")
      assert(params.map(p => extract(p.nameLocation)) == Seq("cacheable", "scope"))
      assert(params.map(p => extract(p.valueLocation)) == Seq("true", "'global'"))
      assert(params.map(p => extract(p.location)) == Seq("cacheable=true", "scope='global'"))
    }

    it("anchors an unnamed value") {
      val params = parameters("@SuppressWarnings('PMD')")
      assert(extract(params.head.nameLocation) == "<none>")
      assert(extract(params.head.valueLocation) == "'PMD'")
      assert(extract(params.head.location) == "'PMD'")
    }

    it("has no value location for a name with nothing assigned") {
      val params = parameters("@Dummy(a= )")
      assert(params.length == 1)
      assert(params.head.name.contains("a"))
      assert(params.head.value.isEmpty)
      assert(params.head.valueLocation.isEmpty)
      assert(extract(params.head.location) == "a=")
    }
  }

  describe("the unparsed parameters string") {
    it("is unchanged for each form") {
      assert(annotation("@AuraEnabled").parameters.isEmpty)
      assert(annotation("@AuraEnabled()").parameters.contains(""))
      assert(annotation("@SuppressWarnings('PMD')").parameters.contains("'PMD'"))
      assert(
        annotation("@AuraEnabled(cacheable=true scope='global')").parameters
          .contains("cacheable=truescope='global'")
      )
      assert(
        annotation("@AuraEnabled(cacheable=true, scope='global')").parameters
          .contains("cacheable=true,scope='global'")
      )
      assert(annotation("@Dummy(a=f(1, 2) b=3)").parameters.contains("a=f(1,2)b=3"))
    }
  }

  describe("equality") {
    it("still ignores locations") {
      val first  = annotation("@AuraEnabled(cacheable=true scope='global')")
      val second = annotation("\n\n  @AuraEnabled(cacheable=true scope='global')")
      assert(first.location != second.location)
      assert(first == second)
      assert(first.hashCode() == second.hashCode())
    }

    it("uses the parameter list, so a lost separator is no longer equal") {
      // Both have the same unparsed 'parameters' string, only the structure tells them apart
      val separated = annotation("@Dummy(a=1 b=2)")
      val run       = annotation("@Dummy(a=1b=2)")
      assert(separated.parameters == run.parameters)
      assert(separated.parameterList != run.parameterList)
      assert(separated != run)
    }

    it("does not treat a hand-built annotation as equal to a parsed one") {
      val parsed = annotation("@AuraEnabled(cacheable=true scope='global')")
      val hand   = Annotation(parsed.name, parsed.parameters)
      assert(hand.parameterList.isEmpty)
      assert(parsed != hand)
    }

    it("ignores the case of names and values, as the unparsed string does") {
      val lower = annotation("@AuraEnabled(cacheable=true scope='global')")
      val upper = annotation("@auraenabled(CACHEABLE=TRUE SCOPE='GLOBAL')")
      assert(lower == upper)
      assert(lower.hashCode() == upper.hashCode())
    }
  }
}
