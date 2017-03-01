/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.casts;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asInteger;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.charAt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lengthLte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import java.util.function.Consumer;

import org.junit.Test;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/*
 *
 * example command line:
 * mx --J @'-Dgraal.Dump=HighTier:1 -Dgraal.MethodFilter=*TestCasts* -Dgraal.TraceTruffleCompilation=true -Dgraal.PrintBackendCFG=false'  junits --tests TestCasts
 *
 * of course, Graal needs to be imported for this to work:
 * DEFAULT_DYNAMIC_IMPORTS=graal-core (or graal-enterprise)
 */
public class TestCasts extends TestBase {

    private static final int TIMEOUT = 15000;

    private abstract static class TestRootNode<T extends Node> extends RootNode {

        private static final FrameDescriptor descriptor = new FrameDescriptor();
        private final String name;
        private boolean isCompiled = false;
        @Child protected T node;

        protected TestRootNode(String name, T node) {
            super(RContext.getRForeignAccessFactory().getTruffleLanguage(), RSyntaxNode.INTERNAL, descriptor);
            this.name = name;
            this.node = node;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object result = execute(frame, frame.getArguments()[0]);
            if (CompilerDirectives.inCompiledCode()) {
                isCompiled = true;
            } else {
                isCompiled = false;
            }
            return result;
        }

        protected abstract Object execute(VirtualFrame frame, Object value);

        @Override
        public String toString() {
            return "TestCasts" + name;
        }
    }

    private static void testCompilation(Object[] values, TestRootNode<?> root, Object... deOptVals) {
        RootCallTarget target = Truffle.getRuntime().createCallTarget(root);

        long timeout = System.currentTimeMillis() + TIMEOUT;
        int i = 0;
        while (System.currentTimeMillis() < timeout) {
            synchronized (TestCasts.class) {
                // synchronized to make sure isCompiled is re-read
                if (root.isCompiled) {
                    break;
                }
            }
            target.call(values[i]);
            i++;
            if (i == values.length) {
                i = 0;
            }
        }
        assert root.isCompiled;

        for (Object deOptVal : deOptVals) {
            target.call(deOptVal);
            assert !root.isCompiled;
        }
    }

    private static CastNode setupAndGetCast(Consumer<CastBuilder> setup) {
        CastBuilder builder = new CastBuilder(1);
        setup.accept(builder);
        return builder.getCasts()[0];
    }

    @Test
    public void testFirstIntegers() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0, "foo").asIntegerVector().findFirst()));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                // use "new Integer(...)" to avoid boxing logic
                return new Integer((int) node.execute(value));
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("FirstInteger"));
        testCompilation(new Object[]{1, 2, RDataFactory.createIntVectorFromScalar(55)}, new Root("FirstIntegerWithVectors"));
        testCompilation(new Object[]{1.2, 2, (byte) 1}, new Root("FirstIntegerWithCoerce"));
    }

    @Test
    public void testFirstIntegerWithConstant() {
        class Root extends TestRootNode<CastNode> {

            private final Object constant;

            protected Root(String name, Object constant) {
                super(name, setupAndGetCast(b -> b.arg(0, "foo").asIntegerVector().findFirst()));
                this.constant = constant;
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                int result = (int) node.execute(constant);
                CompilerAsserts.compilationConstant(result);
                return null;
            }
        }
        testCompilation(new Object[]{1}, new Root("FirstIntegerWithConstant", 1));
        testCompilation(new Object[]{1}, new Root("FirstIntegerWithConstant", 44.5));
        testCompilation(new Object[]{1}, new Root("FirstIntegerWithConstant", (byte) 1));
    }

    @Test
    public void testMustBe() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(integerValue())));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                Object res = node.execute(value);
                return res;
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("MustBeInteger"));
        testCompilation(new Object[]{1, 2, RDataFactory.createIntVectorFromScalar(55)}, new Root("MustBeIntegerWithVectors"));
    }

    @Test
    public void testMapDefaultValue() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mapNull(constant("X"))));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                Object res = node.execute(value);
                return res;
            }
        }
        testCompilation(new Object[]{RNull.instance}, new Root("MapDefaultValueNull"));
        testCompilation(new Object[]{1}, new Root("MapDefaultValueNonNull"));
    }

    @Test
    public void testMapCharAt0() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(String.class).map(charAt0(0))));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                Object res = node.execute(value);
                return res;
            }
        }
        testCompilation(new Object[]{"abc"}, new Root("MapCharAt0NonEmptyString"));
        testCompilation(new Object[]{""}, new Root("MapCharAt0EmptyString"));
    }

    @Test
    public void testMapConstant() {
        class Root extends TestRootNode<CastNode> {

            final boolean mustBeResultCompilationConstant;

            protected Root(String name, boolean mustBeResultCompilationConstant) {
                super(name, setupAndGetCast(b -> b.arg(0).mapIf(instanceOf(Integer.class), constant(10))));
                this.mustBeResultCompilationConstant = mustBeResultCompilationConstant;
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                Object res = node.execute(value);
                if (mustBeResultCompilationConstant) {
                    CompilerAsserts.compilationConstant(res);
                }
                return res;
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("MapConstantInt", true));
        testCompilation(new Object[]{"abc"}, new Root("MapConstantNoInt", false));
    }

    @Test
    public void testMustBeWithConstant() {
        class Root extends TestRootNode<CastNode> {

            private final Object constant;

            protected Root(String name, Object constant) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(integerValue())));
                this.constant = constant;
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                int result = (int) node.execute(constant);
                CompilerAsserts.compilationConstant(result);
                return null;
            }
        }
        testCompilation(new Object[]{1}, new Root("MustBeWithConstant", 1));
    }

    @Test
    public void optimizedBypass() {
        class Root extends TestRootNode<CastNode> {

            private final Object constant;

            protected Root(String name, Object constant) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(integerValue()).asIntegerVector().findFirst()));
                this.constant = constant;
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                int result = (int) node.execute(constant);
                CompilerAsserts.compilationConstant(result);
                return null;
            }
        }
        testCompilation(new Object[]{1}, new Root("optimizeBypass1", 1));
    }

    @Test
    public void testConditionalMapChainWithConstant() {
        class Root extends TestRootNode<CastNode> {

            private final Object constant;

            protected Root(String name, Object constant) {
                super(name, setupAndGetCast(b -> b.arg(0).mapIf(stringValue(), asStringVector()).mapIf(integerValue(), asIntegerVector()).mapIf(logicalValue(), asLogicalVector(),
                                asDoubleVector())));
                this.constant = constant;
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                // return node.execute(value);
                Object result = node.execute(constant);
                CompilerAsserts.compilationConstant(result);
                return null;
            }
        }
        testCompilation(new Object[]{1}, new Root("ConditionalMapChainWithIntegerConstant", 1));
        testCompilation(new Object[]{1}, new Root("ConditionalMapChainWithStringConstant", "aaa"));
        testCompilation(new Object[]{1}, new Root("ConditionalMapChainWithLogicalConstant", RRuntime.LOGICAL_TRUE));
        testCompilation(new Object[]{1}, new Root("ConditionalMapChainWithDoubleConstant1", 1.2));
        testCompilation(new Object[]{1}, new Root("ConditionalMapChainWithDoubleConstant2", Math.PI));
    }

    @Test
    public void testConditionalMapChain() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mapIf(stringValue(), asStringVector()).mapIf(doubleValue(), asDoubleVector()).mapIf(logicalValue(), asLogicalVector(),
                                asIntegerVector())));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("ConditionalMapChainFedByInteger1"), 1.1, "abc", RRuntime.LOGICAL_FALSE);
        testCompilation(new Object[]{1, RDataFactory.createIntVector(new int[]{55, 66}, true),
                        RDataFactory.createIntVectorFromScalar(77)}, new Root(
                                        "ConditionalMapChainFedByInteger2"),
                        1.1, "abc", RRuntime.LOGICAL_FALSE);
        testCompilation(new Object[]{1.1, 2.2, 3.3}, new Root("ConditionalMapChainFedByDouble1"), 1, "abc", RRuntime.LOGICAL_FALSE);
        testCompilation(new Object[]{1.1, RDataFactory.createDoubleVector(new double[]{55.55, 66.66},
                        true), RDataFactory.createDoubleVectorFromScalar(77.77)}, new Root(
                                        "ConditionalMapChainFedByDouble2"),
                        1, "abc", RRuntime.LOGICAL_FALSE);
        testCompilation(new Object[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE,
                        RRuntime.LOGICAL_TRUE}, new Root("ConditionalMapChainFedByLogical1"), 1, "abc", 1.1);
        testCompilation(new Object[]{RRuntime.LOGICAL_FALSE, RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_TRUE}, true),
                        RDataFactory.createLogicalVectorFromScalar(RRuntime.LOGICAL_FALSE)}, new Root("ConditionalMapChainFedByLogical2"), 1, "abc", 1.1);
        testCompilation(new Object[]{"", "abc", "xyz"}, new Root("ConditionalMapChainFedByString1"), 1.1, 1, RRuntime.LOGICAL_FALSE);
        testCompilation(new Object[]{"abc", RDataFactory.createStringVector(new String[]{"", "xyz"},
                        true),
                        RDataFactory.createStringVectorFromScalar("abc")}, new Root("ConditionalMapChainFedByString2"), 1.1, 1, RRuntime.LOGICAL_FALSE);
    }

    @Test
    public void testComplexPipeline2() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst().mustBe(lengthLte(1)).map(
                                charAt0(RRuntime.INT_NA)).replaceNA(100000)));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{RDataFactory.createStringVectorFromScalar("")}, new Root("ComplexPipeline2EmptyString"));
        testCompilation(new Object[]{RDataFactory.createStringVectorFromScalar("a")}, new Root("ComplexPipeline2OneCharString"));
    }

    @Test
    public void testFilterOrExpression() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(stringValue().or(integerValue()))));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("FilterOrExpressionInt"));
        testCompilation(new Object[]{"aaa", "bbb", "ccc"}, new Root("FilterOrString"));
    }

    @Test
    public void testFilterAndExpression() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(instanceOf(Integer.class)).shouldBe(gt0().and(lt(10)))));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{-1, 20, -3}, new Root("FilterAndExpressionOutOfRange"));
    }

    @Test
    public void testFilterNotAndExpression() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(instanceOf(Integer.class)).shouldBe(gt0().and(lt(10)).not())));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{1, 2, 3}, new Root("FilterNotAndExpressionOutOfRange"));
    }

    @Test
    public void testComplexPipeline3() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, setupAndGetCast(b -> b.arg(0).mustBe(numericValue()).asVector().mustBe(singleElement()).findFirst().shouldBe(
                                instanceOf(Byte.class).or(instanceOf(Integer.class).and(gt0())), Message.NON_POSITIVE_FILL).mapIf(instanceOf(Byte.class), asBoolean(),
                                                asInteger())));
            }

            @Override
            protected Object execute(VirtualFrame frame, Object value) {
                @SuppressWarnings("unused")
                Object res = node.execute(value);
                return null;
            }
        }
        testCompilation(new Object[]{RDataFactory.createIntVectorFromScalar(1)}, new Root("ComplexPipeline3SingleInt"));
    }
}
