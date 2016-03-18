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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess.Factory10;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.RInternalError;

public final class RListAccessFactory implements Factory10 {

    private abstract class InteropRootNode extends RootNode {
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
        throw RInternalError.unimplemented("accessIsExecutable");
    }

    @Override
    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return false;
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
        throw RInternalError.unimplemented("accessGetSize");
    }

    @Override
    public CallTarget accessUnbox() {
        throw RInternalError.unimplemented("accessUnbox");
    }

    @Override
    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(ListReadNodeGen.create());
    }

    @Override
    public CallTarget accessWrite() {
        return Truffle.getRuntime().createCallTarget(ListWriteNodeGen.create());
    }

    @Override
    public CallTarget accessExecute(int argumentsLength) {
        throw RInternalError.unimplemented("accessExecute");
    }

    @Override
    public CallTarget accessInvoke(int argumentsLength) {
        return null;
    }

    @Override
    public CallTarget accessNew(int argumentsLength) {
        throw RInternalError.unimplemented("accessNew");
    }

    @Override
    public CallTarget accessMessage(Message unknown) {
        throw RInternalError.unimplemented("accessMessage");
    }
}
