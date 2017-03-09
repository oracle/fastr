/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.TestTrait;

public class TestBuiltin_deriv extends TestBase {

    final class DerivExpr {
        final String expr;
        final String assertedExpr;
        final int dn;
        final boolean hessian;

        DerivExpr(String expr, int dn) {
            this(expr, dn, false);
        }

        DerivExpr(String expr, int dn, boolean hessian) {
            this.expr = expr;
            this.dn = dn;
            this.hessian = hessian;
            String vars = dn == 1 ? "c(\"x\")" : "c(\"x\",\"y\")";
            String h = hessian ? "TRUE" : "FALSE";
            this.assertedExpr = "deriv(~ " + expr + ", " + vars + ", hessian=" + h + ")";
        }

        DerivEval derive() {
            assertEval(assertedExpr);
            return new DerivEval(this);
        }

        DerivEval derive(TestTrait trait) {
            assertEval(trait, assertedExpr);
            return new DerivEval(this);
        }
    }

    final class DerivEval {
        final DerivExpr de;
        final String assertedExpr;

        DerivEval(DerivExpr de) {
            this.de = de;
            String vars = de.dn == 1 ? "x<-%s" : "x<-%s; y<-%s";
            this.assertedExpr = "df <- " + de.assertedExpr + "; " + vars + "; eval(df)";
        }

        DerivEval eval(Object... vals) {
            String ex = String.format(this.assertedExpr, vals);
            assertEval(ex);
            return this;
        }

        DerivEval eval(TestTrait trait, Object... vals) {
            String ex = String.format(this.assertedExpr, vals);
            assertEval(trait, ex);
            return this;
        }

        DerivExpr withHessian() {
            return new DerivExpr(de.expr, de.dn, true);
        }
    }

    private DerivExpr deriv1(String expr) {
        return new DerivExpr(expr, 1);
    }

    private DerivExpr deriv2(String expr) {
        return new DerivExpr(expr, 2);
    }

    private void assertDerivAndEval1(String expr) {
        deriv1(expr).derive().eval(0).withHessian().derive().eval(0);
    }

    private void assertDerivAndEval1(TestTrait trait, String expr) {
        deriv1(expr).derive(trait).eval(0, 0);
    }

    private DerivEval assertDeriv1(String expr) {
        return deriv1(expr).derive();
    }

    private void assertDerivAndEval2(String expr) {
        deriv2(expr).derive().eval(0, 0).withHessian().derive().eval(0, 0);
    }

    @Test
    public void testDeriveBasicExpressions1() {
        assertDerivAndEval1("1");
        assertDerivAndEval1("x");
        assertDerivAndEval1("x+1");
        assertDerivAndEval1("2*x");
        assertDerivAndEval1("x/2");
        assertDerivAndEval1(Ignored.OutputFormatting, "2/x");
        assertDerivAndEval1("x^2");
        assertDerivAndEval1("(x+1)+(x+2)");
        assertDerivAndEval1("(x+1)-(x+2)");
        assertDerivAndEval1("-(x+1)+(x+2)");
        assertDerivAndEval1("-(x+1)-(x+2)");
        assertDerivAndEval1("(x+1)*(x+2)");
        deriv1("(x+1)/(x+2)").derive().eval(0).withHessian().derive(Output.IgnoreWhitespace).eval(0);
        assertDerivAndEval1("(x+1)*(x+2*(x-1))");
        assertDerivAndEval1(Ignored.OutputFormatting, "(x+1)^(x+2)");
    }

    @Test
    public void testDeriveFunctions1() {
        deriv1("log(x)").derive().eval(0).withHessian().derive(Ignored.OutputFormatting).eval(0).eval(1).eval(Ignored.MissingWarning,
                        -1);
        assertDerivAndEval1("exp(x)");
        assertDerivAndEval1("cos(x)");
        assertDerivAndEval1("sin(x)");
        assertDerivAndEval1("tan(x)");
        assertDerivAndEval1("cosh(x)");
        assertDerivAndEval1("sinh(x)");
        deriv1("tanh(x)").derive().eval(0).withHessian().derive(Ignored.OutputFormatting).eval(0).eval(1).eval(-1);
        assertDerivAndEval1("sqrt(x)");
        deriv1("pnorm(x)").derive().eval(0).withHessian().derive(Ignored.OutputFormatting).eval(0);
        assertDerivAndEval1(Ignored.OutputFormatting, "dnorm(x)");
        assertDerivAndEval1("asin(x)");
        assertDerivAndEval1(Ignored.OutputFormatting, "acos(x)");
        deriv1("atan(x)").derive().eval(0).withHessian().derive(Ignored.OutputFormatting).eval(0);
        assertDeriv1("gamma(x)").eval(Ignored.Unimplemented, 0);
        assertDeriv1("lgamma(x)").eval(0.5);
        assertDeriv1("digamma(x)").eval(Ignored.Unimplemented, 0);
        assertDeriv1("trigamma(x)").eval(Ignored.Unimplemented, 0);
        assertDeriv1("psigamma(x)").eval(Ignored.Unimplemented, 0);
    }

    @Test
    public void testDeriveFunctionsWithCompArg1() {
        deriv1("log(2*x)").derive().eval(0).withHessian().derive(Ignored.OutputFormatting).eval(0);
        deriv1("log(sin(2*x))").derive().eval(0).withHessian().derive(Output.IgnoreWhitespace).eval(0);
        assertDerivAndEval1(Output.IgnoreWhitespace, "log(sin(2*x)*cos(x^2))");
        assertDerivAndEval1(Output.IgnoreWhitespace, "pnorm(sin(2*x)^log(x+1))");
    }

    @Test
    public void testDeriveBasicExpressions2() {
        assertDerivAndEval2("x + y");
        deriv2("x*y").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("2*x*y").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("x/y/2").derive(Ignored.OutputFormatting).eval(0,
                        0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("2/x*y").derive(Ignored.OutputFormatting).eval(0,
                        0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("x^y").derive(Ignored.OutputFormatting).eval(0,
                        0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("(x+1)*(y+2)").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0,
                        0);
        assertDerivAndEval2("(x+1)-(y+2)");
        deriv2("-(x+1)+(y+2)").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0,
                        0);
        deriv2("-(x+1)-(y+2)").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0,
                        0);
        deriv2("(x+1)/(y+2)").derive(Ignored.OutputFormatting).eval(0,
                        0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("(x+1)*(y+2*(x-1))").derive().eval(0,
                        0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
        deriv2("(x+1)^(y+2)").derive().eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0,
                        0).eval(1, 1);
    }

    @Test
    public void testLongExpression() {
        deriv2("(log(2*x)+sin(x))*cos(y^x*(exp(x)))*(x*y+x^y/(x+y+1))").derive(Output.IgnoreWhitespace).eval(0, 0).withHessian().derive(Ignored.OutputFormatting).eval(0, 0);
    }

    @Test
    public void testFunctionGenereration() {
        assertEval(Output.IgnoreWhitespace, "(df <- deriv(~x^2*sin(x), \"x\", function.arg=TRUE));df(0)");
        assertEval(Output.IgnoreWhitespace, "(df <- deriv(~x^2*sin(x), \"x\", function.arg=c(\"x\")));df(0)");
        assertEval(Output.IgnoreWhitespace, "(df <- deriv(~x^2*sin(x), \"x\", function.arg=function(x=1){}));df(0)");
    }

    @Test
    public void testUnusualExprs() {
        assertEval("(df <- deriv(expression(x^2*sin(x)), \"x\"));df(0)");
        assertEval("(df <- deriv(quote(x^2*sin(x)), \"x\"));df(0)");
        assertEval("g<-quote(x^2);(df <- deriv(g, \"x\"));df(0)");
        assertEval("(df <- deriv(1, \"x\"));df(0)");
        assertEval("x<-1;(df <- deriv(x, \"x\"));df(0)");
    }

}
