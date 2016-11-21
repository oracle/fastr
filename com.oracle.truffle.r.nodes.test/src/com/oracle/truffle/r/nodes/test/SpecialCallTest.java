/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.test;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.nodes.access.WriteVariableSyntaxNode;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.control.ReplacementDispatchNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RootWithBody;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

public class SpecialCallTest extends TestBase {
    private static final class CountCallsVisitor extends RSyntaxVisitor<Void> {

        public int normal;
        public int special;

        CountCallsVisitor(RootCallTarget callTarget) {
            accept(((RootWithBody) callTarget.getRootNode()).getBody().asRSyntaxNode());
        }

        @Override
        protected Void visit(RSyntaxCall element) {
            if (element instanceof RCallSpecialNode) {
                special++;
            } else if (element instanceof RCallNode) {
                normal++;
            } else {
                assert element instanceof ReplacementDispatchNode || element instanceof WriteVariableSyntaxNode || element instanceof BlockNode : "unexpected node while testing";
            }
            accept(element.getSyntaxLHS());
            for (RSyntaxElement arg : element.getSyntaxArguments()) {
                accept(arg);
            }
            return null;
        }

        @Override
        protected Void visit(RSyntaxConstant element) {
            return null;
        }

        @Override
        protected Void visit(RSyntaxLookup element) {
            return null;
        }

        @Override
        protected Void visit(RSyntaxFunction element) {
            for (RSyntaxElement arg : element.getSyntaxArgumentDefaults()) {
                accept(arg);
            }
            accept(element.getSyntaxBody());
            return null;
        }
    }

    private static final class PrintCallsVisitor extends RSyntaxVisitor<Void> {

        private String indent = "";

        void print(RootCallTarget callTarget) {
            System.out.println();
            accept(((RootWithBody) callTarget.getRootNode()).getBody().asRSyntaxNode());
        }

        @Override
        protected Void visit(RSyntaxCall element) {
            System.out.println(indent + "call " + element.getClass().getSimpleName());
            indent += "    ";
            System.out.println(indent.substring(2) + "lhs:");
            accept(element.getSyntaxLHS());
            printArgs(element.getSyntaxSignature(), element.getSyntaxArguments());
            indent = indent.substring(4);
            return null;
        }

        @Override
        protected Void visit(RSyntaxConstant element) {
            System.out.println(indent + "constant " + element.getClass().getSimpleName() + " " + element.getValue().getClass().getSimpleName() + " " + element.getValue());
            return null;
        }

        @Override
        protected Void visit(RSyntaxLookup element) {
            System.out.println(indent + "lookup " + element.getClass().getSimpleName() + " " + element.getIdentifier());
            return null;
        }

        @Override
        protected Void visit(RSyntaxFunction element) {
            System.out.println(indent + "function " + element.getClass().getSimpleName());
            indent += "    ";
            printArgs(element.getSyntaxSignature(), element.getSyntaxArgumentDefaults());
            indent = indent.substring(4);
            for (RSyntaxElement arg : element.getSyntaxArgumentDefaults()) {
                accept(arg);
            }
            System.out.println(indent.substring(2) + "body:");
            accept(element.getSyntaxBody());
            return null;
        }

        private void printArgs(ArgumentsSignature signature, RSyntaxElement[] arguments) {
            for (int i = 0; i < arguments.length; i++) {
                System.out.println(indent.substring(2) + "arg " + (signature.getName(i) == null ? "<unnamed>" : signature.getName(i)));
                accept(arguments[i]);
            }
        }
    }

    @Test
    public void testBasic() {
        // check a case with no calls
        assertCallCounts("library(stats)", 0, 1, 0, 1);
    }

    @Test
    public void testArithmetic() {
        assertCallCounts("1 + 1", 1, 0, 1, 0);
        assertCallCounts("1 + 1 * 2 + 4", 3, 0, 3, 0);

        assertCallCounts("{ a <- 1; b <- 2; a + b }", 1, 0, 1, 0);
        assertCallCounts("{ a <- 1; b <- 2; c <- 3; a + b * 2 * c}", 3, 0, 3, 0);

        assertCallCounts("{ a <- data.frame(a=1); b <- 2; c <- 3; a + b * 2 * c}", 3, 1, 2, 2);
        assertCallCounts("{ a <- 1; b <- data.frame(a=1); c <- 3; a + b * 2 * c}", 3, 1, 0, 4);

        assertCallCounts("1 %*% 1", 0, 1, 0, 1);
    }

    @Test
    public void testSubset() {
        assertCallCounts("{ a <- 1:10; a[1] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- c(1,2,3,4); a[2] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- c(1,2,3,4); a[4] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- list(c(1,2,3,4),2,3); a[1] }", 1, 2, 1, 2);

        assertCallCounts("{ a <- c(1,2,3,4); a[0.1] }", 1, 1, 0, 2);
        assertCallCounts("{ a <- c(1,2,3,4); a[5] }", 1, 1, 0, 2);
        assertCallCounts("{ a <- c(1,2,3,4); a[0] }", 1, 1, 0, 2);
        assertCallCounts("{ a <- c(1,2,3,4); a[-1] }", 0, 3, 0, 3); // "-1" is a unary expression
        assertCallCounts("{ a <- c(1,2,3,4); b <- -1; a[b] }", 1, 2, 0, 3);
        assertCallCounts("{ a <- c(1,2,3,4); a[NA_integer_] }", 1, 1, 0, 2);
    }

    @Test
    public void testSubscript() {
        assertCallCounts("{ a <- 1:10; a[[1]] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- c(1,2,3,4); a[[2]] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- c(1,2,3,4); a[[4]] }", 1, 1, 1, 1);
        assertCallCounts("{ a <- list(c(1,2,3,4),2,3); a[[1]] }", 1, 2, 1, 2);
        assertCallCounts("{ a <- list(a=c(1,2,3,4),2,3); a[[1]] }", 1, 2, 1, 2);
        assertCallCounts("{ a <- c(1,2,3,4); a[[0.1]] }", 1, 1, 1, 1);

        assertCallCounts("{ a <- c(1,2,3,4); a[[5]] }", 1, 1, 0, 2);
        assertCallCounts("{ a <- c(1,2,3,4); a[[0]] }", 1, 1, 0, 2);
        assertCallCounts("{ a <- c(1,2,3,4); b <- -1; a[[b]] }", 1, 2, 0, 3);
        assertCallCounts("{ a <- c(1,2,3,4); a[[NA_integer_]] }", 1, 1, 0, 2);
    }

    private static void assertCallCounts(String str, int initialSpecialCount, int initialNormalCount, int finalSpecialCount, int finalNormalCount) {
        if (!FastROptions.UseSpecials.getBooleanValue()) {
            return;
        }
        Source source = Source.newBuilder(str).mimeType(TruffleRLanguage.MIME).name("test").build();

        RExpression expression = testVMContext.getThisEngine().parse(source);
        assert expression.getLength() == 1;
        RootCallTarget callTarget = testVMContext.getThisEngine().makePromiseCallTarget(((RLanguage) expression.getDataAt(0)).getRep().asRSyntaxNode().asRNode(), "test");

        try {
            CountCallsVisitor count1 = new CountCallsVisitor(callTarget);
            Assert.assertEquals("initial special call count '" + str + "': ", initialSpecialCount, count1.special);
            Assert.assertEquals("initial normal call count '" + str + "': ", initialNormalCount, count1.normal);

            try {
                callTarget.call(REnvironment.globalEnv().getFrame());
            } catch (RError e) {
                // ignore
            }

            CountCallsVisitor count2 = new CountCallsVisitor(callTarget);
            Assert.assertEquals("special call count after first call '" + str + "': ", finalSpecialCount, count2.special);
            Assert.assertEquals("normal call count after first call '" + str + "': ", finalNormalCount, count2.normal);

            try {
                callTarget.call(REnvironment.globalEnv().getFrame());
            } catch (RError e) {
                // ignore
            }

            CountCallsVisitor count3 = new CountCallsVisitor(callTarget);
            Assert.assertEquals("special call count after second call '" + str + "': ", finalSpecialCount, count3.special);
            Assert.assertEquals("normal call count after second call '" + str + "': ", finalNormalCount, count3.normal);
        } catch (AssertionError e) {
            new PrintCallsVisitor().print(callTarget);
            throw e;
        }
    }
}
