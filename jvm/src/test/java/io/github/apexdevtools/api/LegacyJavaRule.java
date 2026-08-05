/*
 * Copyright (c) 2026 Kevin Jones. All rights reserved.
 */
package io.github.apexdevtools.api;

/** A Rule implementation written against the API shape from before Rule.id() was added. */
public final class LegacyJavaRule implements Rule {
    @Override
    public String name() {
        return "Human-readable name";
    }

    @Override
    public Integer priority() {
        return MAJOR_PRIORITY;
    }
}
