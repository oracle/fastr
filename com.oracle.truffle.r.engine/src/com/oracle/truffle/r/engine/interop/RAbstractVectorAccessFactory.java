/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.Factory18;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.engine.interop.RAbstractVectorAccessFactoryFactory.VectorReadNodeGen;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RContext.RCloseable;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public final class RAbstractVectorAccessFactory implements Factory18 {

    static class VectorSizeNode extends RootNode {

        @Child private RLengthNode lengthNode = RLengthNode.create();

        @SuppressWarnings("deprecation")
        VectorSizeNode() {
            super(TruffleRLanguage.class, null, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return lengthNode.executeInteger(frame, ForeignAccess.getReceiver(frame));
        }
    }

    abstract static class VectorReadNode extends RootNode {

        @CompilationFinal private boolean lengthAccess;
        @Child private ExtractVectorNode extract = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, true);
        @Child private RLengthNode lengthNode = RLengthNode.create();
        @Child private Node findContext = TruffleRLanguage.INSTANCE.actuallyCreateFindContextNode();

        @SuppressWarnings("deprecation")
        VectorReadNode() {
            super(TruffleRLanguage.class, null, null);
            this.lengthAccess = false;
        }

        @Override
        @SuppressWarnings("try")
        public final Object execute(VirtualFrame frame) {
            try (RCloseable c = RContext.withinContext(TruffleRLanguage.INSTANCE.actuallyFindContext0(findContext))) {
                Object label = ForeignAccess.getArguments(frame).get(0);
                Object receiver = ForeignAccess.getReceiver(frame);
                return execute(frame, receiver, label);
            }
        }

        protected abstract Object execute(VirtualFrame frame, Object reciever, Object label);

        @Specialization
        protected Object readIndexed(VirtualFrame frame, Object receiver, int label) {
            return extract.apply(frame, receiver, new Object[]{label + 1}, RLogical.TRUE, RLogical.TRUE);
        }

        @Specialization
        protected Object readProperty(VirtualFrame frame, Object receiver, String label) {
            return extract.applyAccessField(frame, receiver, label);
        }
    }

    private abstract class InteropRootNode extends RootNode {
        @SuppressWarnings("deprecation")
        InteropRootNode() {
            super(TruffleRLanguage.class, null, null);
        }
    }

    @Override
    public CallTarget accessIsNull() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return false;
            }
        });
    }

    @Override
    public CallTarget accessIsExecutable() {
        return null;
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                RAbstractVector arg = (RAbstractVector) ForeignAccess.getReceiver(frame);
                return arg.getLength() == 1;
            }
        });
    }

    @Override
    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return true;
            }
        });
    }

    @Override
    public CallTarget accessGetSize() {
        return Truffle.getRuntime().createCallTarget(new VectorSizeNode());
    }

    @Override
    public CallTarget accessUnbox() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                RAbstractVector arg = (RAbstractVector) ForeignAccess.getReceiver(frame);
                return arg.getDataAtAsObject(0);
            }
        });
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(VectorReadNodeGen.create());
    }

    @Override
    public CallTarget accessWrite() {
        return null;
    }

    @Override
    public CallTarget accessExecute(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessInvoke(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessMessage(Message unknown) {
        return null;
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessKeys() {
        return null;
    }
}
