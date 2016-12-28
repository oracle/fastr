/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.stats;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * There are four R functions for each distribution function, in case of uniform distribution it is:
 * punif, qunif, dunif and runif. The last one is tested in {@link TestRandGenerationFunctions}, the
 * first three are tested here.
 * <p>
 * This test infrastructure uses some properties of those functions: each distribution has some
 * parameters, e.g. uniform has 'min' and 'max', and those are used as parameters for all three --
 * punif, qunif, dunif. First parameters for pxxx and dxxx always have the same meaning (quantiles).
 * First parameter of qxxx is always probability or log(probability) if log.p=TRUE.
 */
public class TestDistributions extends TestBase {
    private static final String[] BOOL_VALUES = new String[]{"T", "F"};
    private static final String[] DEFAULT_Q = new String[]{"-Inf", "-0.42e-30", "0", "0.42e-30", "Inf", "NaN"};
    private static final String PROBABILITIES = "c(0, 42e-80, 0.1, 0.5, 0.7, 1-42e-80, 1)";
    private static final String[] ERROR_PROBABILITIES = new String[]{"Inf", "-Inf", "NaN", "-42", "-0.42e-38"};

    // @formatter:off
    /**
     * For each distribution we define meaningful (to test) combinations of parameters. For each such
     * combination we also define  a list of meaningful (to test) quantile values (first argument to
     * pxxx, and dxxx R functions).
     */
    private static final DistrTest[] testCases = new DistrTest[]{
            distr("unif").
                    test("-3, 3.3", withDefaultQ("-3", "2", "3.3")),
            distr("cauchy").
                    // alpha < 0 => error
                    test("0, -1", withQuantiles("0", "-1", "42")).
                    test("-5, 5", withDefaultQ("-5", "42")).
                    test("-0.01, 0.03", withQuantiles("0", "-1", "1", "0.42e-30")),
            distr("norm").
                    // sd <= 0 => error
                    test("0, -1", withQuantiles("0")).
                    test("0, 0", withDefaultQ("1")).
                    test("4, 4", withQuantiles("4", "-100", "0")),
            distr("gamma").
                    addErrorParamValues("-1", "0").
                    test("1, scale=2", withDefaultQ()).
                    test("11e11, scale=23e-11", withQuantiles("900", "5000", "0")),
            distr("beta").
                    addErrorParamValues("-1", "0").
                    test("0.5, 0.5", withDefaultQ()).
                    test("2, 5", withDefaultQ("0.5")).
                    test("6, 3", withQuantiles("0.6", "0.1", "42e-33")).
                    test("0.1, 3", withDefaultQ("0.2")).
                    // "p==0, q==0, p = Inf, q = Inf <==> treat as one- or two-point mass"
                    test("0, Inf", withDefaultQ("0.5")).
                    test("Inf, 0", withDefaultQ("0.6")).
                    test("0, 0", withDefaultQ("0.4")),
            distr("exp").
                    addErrorParamValues("-1", "0").
                    test("13e-20", withDefaultQ("10", "-10")).
                    test("42", withDefaultQ("42")).
                    test("42e123", withDefaultQ("33e123")),
            // tests for nchisq, which is called in chisq when second param is not missing
            distr("chisq").
                    addErrorParamValues("-3", "0").
                    test("1, 1", withDefaultQ("0.5", "2")).
                    test("420, 4", withQuantiles("0.42e-10", "100", "13e10", "11e111")).
                    test("0.13e-8, 1", withQuantiles("0.42e-10", "100", "13e10", "11e111")).
                    test("1, 0.13e-8", withQuantiles("0.42e-10", "100", "13e10", "11e111")),
            // tests of nbeta, which is called in beta when third param is not missing
            distr("beta").
                    addErrorParamValues("-4", "0").
                    test("10, 15, 0", withDefaultQ("10", "15", "100")).
                    test("7, 13, 3", withDefaultQ("10", "15", "100")).
                    test("7, 11, 0.37e-10", withQuantiles("10", "15", "100")).
                    test("7, 113e11, 1", withQuantiles("10", "15", "100"))
    };
    // @formatter:on

    @Test
    public void testDensityFunctions() {
        for (DistrTest testCase : testCases) {
            for (ParamsAndQuantiles paramsAndQ : testCase.paramsAndQuantiles) {
                testDensityFunction("d" + testCase.name, paramsAndQ.params, paramsAndQ.quantiles);
            }
            testErrorParams("d" + testCase.name, testCase.paramsAndQuantiles.get(0).params, testCase.errorParamValues);
        }
    }

    @Test
    public void testDistributionFunctions() {
        for (DistrTest testCase : testCases) {
            for (ParamsAndQuantiles paramsAndQ : testCase.paramsAndQuantiles) {
                testDistributionFunction("p" + testCase.name, paramsAndQ.params, paramsAndQ.quantiles);
            }
            testErrorParams("p" + testCase.name, testCase.paramsAndQuantiles.get(0).params, testCase.errorParamValues);
        }
    }

    @Test
    public void testQuantileFunctions() {
        for (DistrTest testCase : testCases) {
            String func = "q" + testCase.name;
            for (ParamsAndQuantiles paramsAndQ : testCase.paramsAndQuantiles) {
                testQuantileFunction(func, paramsAndQ.params, PROBABILITIES, "F");
                testQuantileFunction(func, paramsAndQ.params, "log(" + PROBABILITIES + ")", "T");
            }
            String validParams = testCase.paramsAndQuantiles.get(0).params;
            assertEval(Output.MayIgnoreWarningContext, template(func + "(%0, " + validParams + ")", ERROR_PROBABILITIES));
            testErrorParams(func, validParams, testCase.errorParamValues);
        }
    }

    private void testErrorParams(String func, String paramsTemplate, ArrayList<String> errorParamValues) {
        String[] validParams = paramsTemplate.split(",");
        for (int i = 0; i < validParams.length; i++) {
            String[] newParams = Arrays.copyOf(validParams, validParams.length);
            for (String errVal : errorParamValues) {
                newParams[i] = errVal;
                assertEval(Output.MayIgnoreWarningContext, func + "(0, " + String.join(", ", newParams) + ")");
            }
        }
    }

    private void testDensityFunction(String func, String params, String[] quantiles) {
        // creates code like 'qunif(c(1, 2), -3, 3, log=%0)' where '-3,3' is params and
        // 1, 2 are quantiles, template then creates two tests with %0=T and %0=F
        String qVector = "c(" + String.join(", ", quantiles) + ")";
        String code = func + "(" + qVector + ", " + params + ", log=%0)";
        assertEval(Output.MayIgnoreWarningContext, template(code, BOOL_VALUES));
    }

    private void testDistributionFunction(String func, String params, String[] quantiles) {
        String qVector = "c(" + String.join(", ", quantiles) + ")";
        String code = func + "(" + qVector + ", " + params + ", lower.tail=%0, log.p=%1)";
        assertEval(Output.MayIgnoreWarningContext, template(code, BOOL_VALUES, BOOL_VALUES));
    }

    private void testQuantileFunction(String func, String params, String probabilities, String logP) {
        String code = func + "(" + probabilities + ", " + params + ", lower.tail=%0, log.p=" + logP + ")";
        assertEval(Output.MayIgnoreWarningContext, template(code, BOOL_VALUES));
    }

    private static DistrTest distr(String name) {
        return new DistrTest(name);
    }

    private static String[] withQuantiles(String... quantiles) {
        return quantiles;
    }

    private static String[] withDefaultQ(String... quantiles) {
        String[] result = Arrays.copyOf(quantiles, quantiles.length + DEFAULT_Q.length);
        System.arraycopy(DEFAULT_Q, 0, result, quantiles.length, DEFAULT_Q.length);
        return result;
    }

    /**
     * Represents a collection of test parameters for testing a distribution with given
     * {@link #name}.
     */
    private static final class DistrTest {
        public final String name;
        public final ArrayList<ParamsAndQuantiles> paramsAndQuantiles = new ArrayList<>();
        private int paramsCount = -1;
        /**
         * Set of single R values that are supposed to produce error when used as any of the
         * parameters.
         */
        public final ArrayList<String> errorParamValues = new ArrayList<>();

        DistrTest(String name) {
            this.name = name;
            addErrorParamValues("-Inf", "Inf", "NaN");
        }

        public DistrTest test(String params, String[] quantiles) {
            assert paramsCount == -1 || params.split(",").length == paramsCount : "different length of params for " + name;
            paramsCount = params.split(",").length;
            paramsAndQuantiles.add(new ParamsAndQuantiles(params, quantiles));
            return this;
        }

        public DistrTest addErrorParamValues(String... values) {
            errorParamValues.addAll(Arrays.asList(values));
            return this;
        }

        public DistrTest clearDefaultErrorParamValues() {
            errorParamValues.clear();
            return this;
        }
    }

    /**
     * A combination of params, e.g. "3, 10", with set of quantiles that should be tested with it.
     */
    private static final class ParamsAndQuantiles {
        public final String params;
        public final String[] quantiles;

        ParamsAndQuantiles(String params, String[] quantiles) {
            this.params = params;
            this.quantiles = quantiles;
        }
    }
}
