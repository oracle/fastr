/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi;

import static com.oracle.truffle.r.runtime.context.FastROptions.UseInternalGridGraphics;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * Miscellaneous methods implemented in native code.
 *
 */
public final class MiscRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public MiscRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static final class ExactSumNode extends NativeCallNode {
        private ExactSumNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public double execute(double[] values, boolean hasNa, boolean naRm) {
            return (double) call(NativeFunction.exactSumFunc, values, values.length, hasNa ? 1 : 0, naRm ? 1 : 0);
        }

        public static ExactSumNode create() {
            return RFFIFactory.getMiscRFFI().createExactSumNode();
        }
    }

    public static final class DqrlsNode extends NativeCallNode {
        private DqrlsNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            call(NativeFunction.dqrls, x, n, p, y, ny, tol, b, rsd, qty, k, jpvt, qraux, work);
        }

        public static DqrlsNode create() {
            return RFFIFactory.getMiscRFFI().createDqrlsNode();
        }
    }

    public abstract static class AbstractBeforeGraphicsOpNode extends NativeCallNode {

        protected AbstractBeforeGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public abstract int execute();

    }

    public static final class DefaultBeforeGraphicsOpNode extends AbstractBeforeGraphicsOpNode {

        private DefaultBeforeGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent);
        }

        @Override
        public int execute() {
            RContext.checkPendingRepaintRequest();
            return (int) call(NativeFunction.before_graphics_op);
        }

    }

    public static final class DummyBeforeGraphicsOpNode extends AbstractBeforeGraphicsOpNode {

        private DummyBeforeGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent);
        }

        @Override
        public int execute() {
            return 0;
        }

    }

    public abstract static class AbstractAfterGraphicsOpNode extends NativeCallNode {

        private AbstractAfterGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public abstract int execute(Object op, Object args, int record);

    }

    public static final class DefaultAfterGraphicsOpNode extends AbstractAfterGraphicsOpNode {

        @Child protected FFIToNativeMirrorNode opToNativeMirrorNode = FFIToNativeMirrorNode.create();
        @Child protected FFIToNativeMirrorNode argsToNativeMirrorNode = FFIToNativeMirrorNode.create();

        private DefaultAfterGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent);
        }

        @Override
        public int execute(Object op, Object args, int record) {
            return (int) call(NativeFunction.after_graphics_op, opToNativeMirrorNode.execute(op), argsToNativeMirrorNode.execute(args), record);
        }

    }

    public static final class DummyAfterGraphicsOpNode extends AbstractAfterGraphicsOpNode {

        private DummyAfterGraphicsOpNode(DownCallNodeFactory parent) {
            super(parent);
        }

        @Override
        public int execute(Object op, Object args, int record) {
            return 0;
        }

    }

    public static final class JavaGDResizeCallNode extends NativeCallNode {
        @Child protected FFIMaterializeNode materializeNode = FFIMaterializeNode.create();
        @Child protected FFIToNativeMirrorNode opToNativeMirrorNode = FFIToNativeMirrorNode.create();

        private JavaGDResizeCallNode(DownCallNodeFactory parent) {
            super(parent.createDownCallNode());
        }

        public Object execute(VirtualFrame frame, Object dev) {
            return call(frame, NativeFunction.javaGDresizeCall, opToNativeMirrorNode.execute(materializeNode.materialize(dev)));
        }

    }

    public ExactSumNode createExactSumNode() {
        return new ExactSumNode(downCallNodeFactory);
    }

    public DqrlsNode createDqrlsNode() {
        return new DqrlsNode(downCallNodeFactory);
    }

    public AbstractBeforeGraphicsOpNode createBeforeGraphicsOpNode() {
        return RContext.getInstance().getOption(UseInternalGridGraphics) ? new DummyBeforeGraphicsOpNode(downCallNodeFactory) : new DefaultBeforeGraphicsOpNode(downCallNodeFactory);
    }

    public AbstractAfterGraphicsOpNode createAfterGraphicsOpNode() {
        return RContext.getInstance().getOption(UseInternalGridGraphics) ? new DummyAfterGraphicsOpNode(downCallNodeFactory) : new DefaultAfterGraphicsOpNode(downCallNodeFactory);
    }

    public JavaGDResizeCallNode createJavaGDResizeCallNode() {
        return new JavaGDResizeCallNode(downCallNodeFactory);
    }
}
