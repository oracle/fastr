/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

public enum RGroupGenerics {
    Math,
    Ops,
    Summary,
    Complex;

    public String getName() {
        return name();
    }

    public static RGroupGenerics getGroup(String methodName) {
        switch (methodName) {
            case "abs":
            case "sign":
            case "sqrt":
            case "floor":
            case "ceiling":
            case "trunc":
            case "round":
            case "signif":
            case "exp":
            case "log":
            case "expm1":
            case "log1p":
            case "cos":
            case "sin":
            case "tan":
            case "acos":
            case "asin":
            case "atan":
            case "cosh":
            case "sinh":
            case "tanh":
            case "acosh":
            case "asinh":
            case "atanh":
            case "lgamma":
            case "gamma":
            case "digamma":
            case "trigamma":
            case "cumsum":
            case "cumprod":
            case "cummax":
            case "cummin":
                return Math;
            case "+":
            case "-":
            case "*":
            case "/":
            case "^":
            case "%%":
            case "%/%":
            case "&":
            case "|":
            case "!":
            case "==":
            case "!=":
            case "<":
            case "<=":
            case ">=":
            case ">":
                return Ops;
            case "max":
            case "min":
            case "prod":
            case "sum":
            case "all":
            case "any":
            case "range":
                return Summary;
            case "Arg":
            case "Conj":
            case "Im":
            case "Mod":
            case "Re":
                return Complex;
            default:
                return null;
        }
    }

    public static boolean isGroupGeneric(String methodName) {
        return getGroup(methodName) != null;
    }
}
