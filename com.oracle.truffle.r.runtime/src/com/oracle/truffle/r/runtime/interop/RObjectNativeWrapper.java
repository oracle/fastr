/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.ForeignAccess.StandardFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RObject;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class RObjectNativeWrapper implements TruffleObject {
    private final RObject obj;

    public RObjectNativeWrapper(RObject obj) {
        this.obj = obj;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return ForeignAccess.create(RObjectNativeWrapper.class, new StandardFactory() {
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
            public CallTarget accessIsPointer() {
                return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        return true;
                    }
                });
            }

            @Override
            public CallTarget accessAsPointer() {
                return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        RObjectNativeWrapper receiver = (RObjectNativeWrapper) ForeignAccess.getReceiver(frame);
                        return NativeDataAccess.asPointer(receiver.obj);
                    }
                });
            }

            @Override
            public CallTarget accessToNative() {
                return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
                    @Override
                    public Object execute(VirtualFrame frame) {
                        return ForeignAccess.getReceiver(frame);
                    }
                });
            }
        });
    }
}

abstract class InteropRootNode extends RootNode {
    InteropRootNode() {
        super(null);
    }

    @Override
    public final SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}
