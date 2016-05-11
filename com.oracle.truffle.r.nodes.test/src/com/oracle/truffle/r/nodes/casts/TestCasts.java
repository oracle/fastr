/*
 * please DO NOT PUSH this file to fastr/master without further discussion.
 *
 * example command line:
 * mx --J @'-Dgraal.Dump=HighTier:1 -Dgraal.MethodFilter=*TestCasts* -Dgraal.TraceTruffleCompilation=true -Dgraal.PrintBackendCFG=false'  junits --tests TestCasts
 *
 * of course, Graal needs to be imported for this to work:
 * DEFAULT_DYNAMIC_IMPORTS=graal-core (or graal-enterprise)
 */
package com.oracle.truffle.r.nodes.casts;

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
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

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
            }
            return result;
        }

        protected abstract Object execute(VirtualFrame frame, Object value);

        @Override
        public String toString() {
            return "TestCasts" + name;
        }
    }

    private static void testCompilation(Object[] values, TestRootNode<?> root) {
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
    }

    @Test
    public void testFirstIntegers() {
        class Root extends TestRootNode<CastNode> {

            protected Root(String name) {
                super(name, new CastBuilder().firstIntegerWithError(0, Message.INVALID_ARGUMENT, "foo").getCasts()[0]);
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
                super(name, new CastBuilder().firstIntegerWithError(0, Message.INVALID_ARGUMENT, "foo").getCasts()[0]);
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
}
