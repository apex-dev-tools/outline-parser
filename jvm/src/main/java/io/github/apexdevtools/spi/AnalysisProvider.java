/*
 * Copyright (c) 2022 FinancialForce.com, inc. All rights reserved.
 */
package io.github.apexdevtools.spi;

import io.github.apexdevtools.api.Issue;

import java.nio.file.Path;
import java.util.List;

/**
 * Service provider for an external analysis that can return Issues.
 * The Provider may throw on bad input.
 */
public interface AnalysisProvider {

    /**
     * Return an identifier for the provider, these need to be unique across all providers.
     */
    String getProviderId();

    /**
     * Set provider configuration items, these need to fit a key->value(s) model.
     *
     * @param name   name of configuration parameter
     * @param values optional list of values to use with parameter
     */
    void setConfiguration(String name, List<String> values);

    /**
     * Test if configured correctly for use in workspace
     *
     * @param workspacePath workspace to run analysis in
     */
    Boolean isConfigured(Path workspacePath);

    /**
     * Return issues for the set of files.
     *
     * @param workspacePath workspace to run analysis in
     * @param files         files within workspace to analyse
     */
    Issue[] collectIssues(Path workspacePath, Path[] files);
}
