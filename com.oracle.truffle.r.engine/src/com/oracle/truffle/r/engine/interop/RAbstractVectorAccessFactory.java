/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.interop.ForeignAccess.Factory10;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;

public final class RAbstractVectorAccessFactory implements Factory10 {

    public abstract class InteropRootNode extends RootNode {
        public InteropRootNode() {
            super(TruffleRLanguage.class, null, null);
        }
    }

    public CallTarget accessIsNull() {
        throw RInternalError.shouldNotReachHere("message: accessIsNull");
    }

    public CallTarget accessIsExecutable() {
        throw RInternalError.shouldNotReachHere("message: accessIsExecutable");
    }

    public CallTarget accessIsBoxed() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                RAbstractVector arg = (RAbstractVector) ForeignAccess.getReceiver(frame);
                return arg.getLength() == 1;
            }
        });
    }

    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return true;
            }
        });
    }

    public CallTarget accessGetSize() {
        return Truffle.getRuntime().createCallTarget(new VectorSizeNode());
    }

    public CallTarget accessUnbox() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                RAbstractVector arg = (RAbstractVector) ForeignAccess.getReceiver(frame);
                return arg.getDataAtAsObject(0);
            }
        });
    }

    public CallTarget accessRead() {
        return Truffle.getRuntime().createCallTarget(new VectorReadNode());
    }

    public CallTarget accessWrite() {
        throw RInternalError.shouldNotReachHere("message: accessWrite");
    }

    public CallTarget accessExecute(int argumentsLength) {
        throw RInternalError.shouldNotReachHere("message: accessExecute");
    }

    public CallTarget accessInvoke(int argumentsLength) {
        throw RInternalError.shouldNotReachHere("message: accessInvoke");
    }

    public CallTarget accessMessage(Message unknown) {
        throw RInternalError.shouldNotReachHere("message: " + unknown);
    }
    
    @SuppressWarnings("all")
    public CallTarget accessNew(int argumentsLength) {
        return null;
    }
}
