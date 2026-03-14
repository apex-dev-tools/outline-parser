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
 * A diagnostic issue.
 * Each issue must identify which provider generated it, the file path & location it refers to,
 * a Rule the issue relates to, if the issue should be considered an error and message for
 * the issue. This is based on the PMD model of a diagnostic issue.
 * WARNING: This must be identical to Scala class of same name
 */
public abstract class Issue {

    /**
     * Name of provider which created this Issue
     */
    public abstract String provider();

    /**
     * The file path where the issue was found
     */
    public abstract String filePath();

    /**
     * The location within the file
     */
    public abstract IssueLocation fileLocation();

    /**
     * The rule violated, sets the issue priority
     */
    public abstract Rule rule();

    /**
     * Is this considered an error issue, rather than a warning
     */
    public abstract Boolean isError();

    /**
     * The issue message
     */
    public abstract String message();

    /**
     * Format as String, filePath is omitted to avoid duplicating over multiple Issues
     */
    public String asString() {
        return rule().name() + ": " + fileLocation().displayPosition() + ": " + message();
    }

    @Override
    public String toString() {
        return filePath() + ": " + asString();
    }
}
