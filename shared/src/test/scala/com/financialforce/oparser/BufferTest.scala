/*
 * Copyright (c) 2026 FinancialForce.com, inc. All rights reserved.
 */

package com.financialforce.oparser

import com.financialforce.types.base.Location
import org.scalatest.funspec.AnyFunSpec

import java.nio.charset.StandardCharsets

class BufferTest extends AnyFunSpec {

  describe("Buffer byte offset calculation") {

    it("should correctly calculate byte offsets for ASCII characters") {
      val content = "ABC"
      val buffer  = new Buffer(content)

      // Append 3 ASCII characters (1 byte each)
      // charOffset matches string positions, byteOffset tracks UTF-8 byte positions
      buffer.append(byteOffset = 0, charOffset = 0, line = 0, lineOffset = 0, charByteLength = 1)
      buffer.append(byteOffset = 1, charOffset = 1, line = 0, lineOffset = 1, charByteLength = 1)
      buffer.append(byteOffset = 2, charOffset = 2, line = 0, lineOffset = 2, charByteLength = 1)

      val (captured, location) = buffer.captured()

      assert(captured == "ABC")
      assert(location.startByteOffset == 0)
      assert(location.endByteOffset == 3) // After last character (exclusive)
    }

    it("should correctly calculate byte offsets for 2-byte UTF-8 characters") {
      val content = "café" // é is 2 bytes in UTF-8
      val buffer  = new Buffer(content)

      // c=1 byte, a=1 byte, f=1 byte, é=2 bytes
      buffer.append(byteOffset = 0, charOffset = 0, line = 0, lineOffset = 0, charByteLength = 1)
      buffer.append(byteOffset = 1, charOffset = 1, line = 0, lineOffset = 1, charByteLength = 1)
      buffer.append(byteOffset = 2, charOffset = 2, line = 0, lineOffset = 2, charByteLength = 1)
      buffer.append(byteOffset = 3, charOffset = 3, line = 0, lineOffset = 3, charByteLength = 2) // é is 2 bytes

      val (captured, location) = buffer.captured()

      assert(captured == "café")
      assert(location.startByteOffset == 0)
      assert(location.endByteOffset == 5) // 1+1+1+2 = 5 bytes total
    }

    it("should correctly calculate byte offsets for 3-byte UTF-8 characters") {
      val content = "中文" // Each Chinese character is 3 bytes in UTF-8
      val buffer  = new Buffer(content)

      // 中=3 bytes, 文=3 bytes
      buffer.append(byteOffset = 0, charOffset = 0, line = 0, lineOffset = 0, charByteLength = 3)
      buffer.append(byteOffset = 3, charOffset = 1, line = 0, lineOffset = 1, charByteLength = 3)

      val (captured, location) = buffer.captured()

      assert(captured == "中文")
      assert(location.startByteOffset == 0)
      assert(location.endByteOffset == 6) // 3+3 = 6 bytes total
    }

    it("should correctly calculate byte offsets for 4-byte UTF-8 characters (emoji)") {
      val content = "A🤦B" // 🤦 is a surrogate pair, 4 bytes in UTF-8
      val buffer  = new Buffer(content)

      // A=1 byte, 🤦=4 bytes (spans 2 char positions), B=1 byte
      buffer.append(byteOffset = 0, charOffset = 0, line = 0, lineOffset = 0, charByteLength = 1)
      buffer.append(byteOffset = 1, charOffset = 1, line = 0, lineOffset = 1, charByteLength = 4, charCount = 2) // emoji
      buffer.append(byteOffset = 5, charOffset = 3, line = 0, lineOffset = 2, charByteLength = 1) // B at char position 3

      val (captured, location) = buffer.captured()

      assert(captured == "A🤦B")
      assert(location.startByteOffset == 0)
      assert(location.endByteOffset == 6) // 1+4+1 = 6 bytes total
    }

    it("should correctly calculate byte offsets for mixed ASCII and multi-byte characters") {
      val content = "Hi😊" // H=1, i=1, 😊=4 bytes in UTF-8 (but 2 UTF-16 chars!)
      val buffer  = new Buffer(content)

      // In Java Strings (UTF-16), emoji is 2 chars: high surrogate (index 2) + low surrogate (index 3)
      buffer.append(byteOffset = 0, charOffset = 0, line = 0, lineOffset = 0, charByteLength = 1) // 'H'
      buffer.append(byteOffset = 1, charOffset = 1, line = 0, lineOffset = 1, charByteLength = 1) // 'i'
      // Emoji: starts at charOffset 2, spans 2 char positions (surrogate pair), 4 bytes in UTF-8
      buffer.append(byteOffset = 2, charOffset = 2, line = 0, lineOffset = 2, charByteLength = 4, charCount = 2)

      val (captured, location) = buffer.captured()

      assert(captured == "Hi😊") // Now correctly captures the full emoji!
      assert(location.startByteOffset == 0)
      assert(location.endByteOffset == 6) // 1+1+4 = 6 bytes
    }

    it("should handle clearing and reusing the buffer") {
      val content = "Test"
      val buffer  = new Buffer(content)

      buffer.append(0, 0, 0, 0, 1)
      buffer.append(1, 1, 0, 1, 1)

      val (captured1, location1) = buffer.captured()
      assert(captured1 == "Te")
      assert(location1.endByteOffset == 2)

      buffer.clear()
      buffer.append(2, 2, 0, 2, 1)
      buffer.append(3, 3, 0, 3, 1)

      val (captured2, location2) = buffer.captured()
      assert(captured2 == "st")
      assert(location2.startByteOffset == 2)
      assert(location2.endByteOffset == 4)
    }

    it("should verify byte offsets match actual UTF-8 encoding") {
      // This test verifies our byte offset calculation matches actual UTF-8 encoding
      val testCases = Seq(
        ("A", 1),       // ASCII
        ("é", 2),       // 2-byte UTF-8
        ("中", 3),       // 3-byte UTF-8
        ("🤦", 4)        // 4-byte UTF-8 (surrogate pair)
      )

      testCases.foreach { case (char, expectedBytes) =>
        val actualBytes = char.getBytes(StandardCharsets.UTF_8).length
        assert(
          actualBytes == expectedBytes,
          s"Character '$char' should be $expectedBytes bytes but was $actualBytes"
        )
      }
    }
  }
}