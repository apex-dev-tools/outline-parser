/*
 Copyright (c) 2021 Kevin Jones, All rights reserved.
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.
 */

package io.github.apexdevtools.api

/* WARNING: This must be identical to Java class of same name */
trait Issue {

  /* The generator of this issue, default to APEX_LS_PROVIDER */
  def provider(): String

  /* The file path where the issue was found */
  def filePath(): String

  /* The location within the file */
  def fileLocation(): IssueLocation

  /* The rule violated, sets the issue priority */
  def rule(): Rule

  /* The issue message */
  def message(): String

  /* Is this considered an error issue, rather than a warning */
  def isError: java.lang.Boolean

  /* Format as String, filePath is omitted to avoid duplicating over multiple Issues */
  def asString: String = rule().name() + ": " + fileLocation().displayPosition + ": " + message()

  override def toString: String = filePath() + ": " + asString
}

object Issue {
  // Default source name for apex-ls generated diagnostics
  final val APEX_LS_PROVIDER = "apex-ls"
}
