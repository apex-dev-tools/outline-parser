/*
 * Copyright (c) 2026 Kevin Jones. All rights reserved.
 */
package io.github.apexdevtools.api

import org.scalatest.funspec.AnyFunSpec

class RuleTest extends AnyFunSpec {

  it("uses name as the ID for a legacy Scala.js Rule implementation") {
    val rule = new Rule {
      override def name(): String      = "Human-readable name"
      override def priority(): Integer = Rule.MAJOR_PRIORITY
    }

    assert(rule.id() == "Human-readable name")
  }

  it("allows a stable ID to differ from the human-readable name") {
    val rule = new Rule {
      override def id(): String        = "missing-type"
      override def name(): String      = "Missing Type"
      override def priority(): Integer = Rule.MAJOR_PRIORITY
    }

    assert(rule.id() == "missing-type")
    assert(rule.name() == "Missing Type")
  }

  it("keeps Issue string output based on the human-readable name") {
    val issue = new TestIssue

    assert(issue.asString == "Missing Type: line 2 at 3-4: Unknown type 'Example'")
    assert(issue.toString == "Example.cls: Missing Type: line 2 at 3-4: Unknown type 'Example'")
  }

  private class TestIssue extends Issue {
    override def provider(): String = "test"
    override def filePath(): String = "Example.cls"
    override def fileLocation(): IssueLocation = new IssueLocation {
      override def startLineNumber(): Int = 2
      override def startCharOffset(): Int = 3
      override def endLineNumber(): Int   = 2
      override def endCharOffset(): Int   = 4
    }
    override def rule(): Rule = new Rule {
      override def id(): String        = "missing-type"
      override def name(): String      = "Missing Type"
      override def priority(): Integer = Rule.MAJOR_PRIORITY
    }
    override def isError: java.lang.Boolean = true
    override def message(): String          = "Unknown type 'Example'"
  }
}
