/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import java.util.*;

public class RGroupGenerics {

    public static final String RDotGroup = ".Group";

    public static final String GROUP_MATH = "Math";

    public static final String GROUP_OPS = "Ops";

    public static final String GROUP_SUMMARY = "Summary";

    public static final String GROUP_COMPLEX = "Complex";

    private static Map<String, String> methodToGroup;

    /*
     * Returns the group to which a given S-3 group generic method belongs to.
     */
    public static String getGroup(final String methodName) {
        assert (methodName != null);
        if (methodToGroup == null) {
            initializeMethodToGroup();
        }
        return methodToGroup.get(methodName);
    }

    /*
     * S3 methods can be written for four groups:"Math", "Ops", "Summary" and "Complex". The
     * following method maps each method to its associated group.
     */
    private static void initializeMethodToGroup() {
        assert (methodToGroup == null);
        methodToGroup = new HashMap<>();
        addGroupMath();
        addGroupOps();
        addGroupSummary();
        addGroupComplex();
    }

    private static void addGroupMath() {
        methodToGroup.put("abs", GROUP_MATH);
        methodToGroup.put("sign", GROUP_MATH);
        methodToGroup.put("sqrt", GROUP_MATH);
        methodToGroup.put("floor", GROUP_MATH);
        methodToGroup.put("ceiling", GROUP_MATH);
        methodToGroup.put("trunc", GROUP_MATH);
        methodToGroup.put("round", GROUP_MATH);
        methodToGroup.put("signif", GROUP_MATH);
        methodToGroup.put("exp", GROUP_MATH);
        methodToGroup.put("log", GROUP_MATH);
        methodToGroup.put("expm1", GROUP_MATH);
        methodToGroup.put("log1p", GROUP_MATH);
        methodToGroup.put("cos", GROUP_MATH);
        methodToGroup.put("sin", GROUP_MATH);
        methodToGroup.put("tan", GROUP_MATH);
        methodToGroup.put("acos", GROUP_MATH);
        methodToGroup.put("asin", GROUP_MATH);
        methodToGroup.put("atan", GROUP_MATH);
        methodToGroup.put("cosh", GROUP_MATH);
        methodToGroup.put("sinh", GROUP_MATH);
        methodToGroup.put("tanh", GROUP_MATH);
        methodToGroup.put("acosh", GROUP_MATH);
        methodToGroup.put("asinh", GROUP_MATH);
        methodToGroup.put("atanh", GROUP_MATH);
        methodToGroup.put("lgamma", GROUP_MATH);
        methodToGroup.put("gamma", GROUP_MATH);
        methodToGroup.put("digamma", GROUP_MATH);
        methodToGroup.put("trigamma", GROUP_MATH);
        methodToGroup.put("cumsum", GROUP_MATH);
        methodToGroup.put("cumprod", GROUP_MATH);
        methodToGroup.put("cummax", GROUP_MATH);
        methodToGroup.put("cummin", GROUP_MATH);
    }

    private static void addGroupOps() {
        methodToGroup.put("+", GROUP_OPS);
        methodToGroup.put("-", GROUP_OPS);
        methodToGroup.put("*", GROUP_OPS);
        methodToGroup.put("/", GROUP_OPS);
        methodToGroup.put("^", GROUP_OPS);
        methodToGroup.put("%%", GROUP_OPS);
        methodToGroup.put("%/%", GROUP_OPS);
        methodToGroup.put("&", GROUP_OPS);
        methodToGroup.put("|", GROUP_OPS);
        methodToGroup.put("!", GROUP_OPS);
        methodToGroup.put("==", GROUP_OPS);
        methodToGroup.put("!=", GROUP_OPS);
        methodToGroup.put("<", GROUP_OPS);
        methodToGroup.put("<=", GROUP_OPS);
        methodToGroup.put(">=", GROUP_OPS);
        methodToGroup.put(">", GROUP_OPS);
    }

    private static void addGroupSummary() {
        methodToGroup.put("max", GROUP_SUMMARY);
        methodToGroup.put("min", GROUP_SUMMARY);
        methodToGroup.put("prod", GROUP_SUMMARY);
        methodToGroup.put("sum", GROUP_SUMMARY);
        methodToGroup.put("all", GROUP_SUMMARY);
        methodToGroup.put("any", GROUP_SUMMARY);
        methodToGroup.put("range", GROUP_SUMMARY);
    }

    private static void addGroupComplex() {
        methodToGroup.put("Arg", GROUP_COMPLEX);
        methodToGroup.put("Conj", GROUP_COMPLEX);
        methodToGroup.put("Im", GROUP_COMPLEX);
        methodToGroup.put("Mod", GROUP_COMPLEX);
        methodToGroup.put("Re", GROUP_COMPLEX);
    }

}
