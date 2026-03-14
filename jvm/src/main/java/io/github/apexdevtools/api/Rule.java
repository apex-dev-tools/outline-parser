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

package io.github.apexdevtools.api;

/**
 * Analysis named rule.
 * Provides basic information about the Rule.
 * WARNING: This must be identical to Scala class of same name
 */
public interface Rule {
    // Priority constants, these are based on SonarQube, PMD uses Integer 1-5

    /**
     * Change absolutely required
     */
    Integer BLOCKER_PRIORITY = 1;
    /**
     * Change highly recommended
     */
    Integer CRITICAL_PRIORITY = 2;
    /**
     * Change recommended
     */
    Integer MAJOR_PRIORITY = 3;
    /**
     * Change optional
     */
    Integer MINOR_PRIORITY = 4;
    /**
     * Change highly optional
     */
    Integer INFO_PRIORITY = 5;

    /**
     * The nane of the rule, does need to be unique but recommended it is
     */
    String name();

    /**
     * Priority Range 1-5, 1 being the highest
     */
    Integer priority();
}
