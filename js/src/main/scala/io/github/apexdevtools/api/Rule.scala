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
trait Rule {
  /* The nane of the rule */
  def name(): String

  /* Range 1-5, 1 being the highest */
  def priority(): Integer
}

object Rule {
  // Priority constants, these should become enums after moving to Scala3
  // These are based on SonarQube, PMD uses Integer 1-5
  final val BLOCKER_PRIORITY  = 1 // Change absolutely required
  final val CRITICAL_PRIORITY = 2 // Change highly recommended
  final val MAJOR_PRIORITY    = 3 // Change recommended
  final val MINOR_PRIORITY    = 4 // Change optional
  final val INFO_PRIORITY     = 5 // Change highly optional
}
