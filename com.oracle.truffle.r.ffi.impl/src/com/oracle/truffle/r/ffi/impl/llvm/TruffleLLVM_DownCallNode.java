/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.ffi.impl.common.DownCallNode;
import com.oracle.truffle.r.ffi.impl.interop.NativeDoubleArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeIntegerArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeNACheck;
import com.oracle.truffle.r.runtime.ffi.DLL;

public abstract class TruffleLLVM_DownCallNode extends DownCallNode {

    @Override
    protected final TruffleObject getTarget() {
        CompilerAsserts.neverPartOfCompilation();
        return DLL.findSymbol(getFunction().getCallName(), null).asTruffleObject();
    }

    @Override
    @ExplodeLoop
    protected void wrapArguments(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            Object obj = args[i];
            if (obj instanceof double[]) {
                args[i] = new NativeDoubleArray((double[]) obj);
            } else if (obj instanceof int[]) {
                args[i] = new NativeIntegerArray((int[]) obj);
            } else if (obj == null) {
                args[i] = 0;
            }
        }
    }

    @Override
    @ExplodeLoop
    protected void finishArguments(Object[] args) {
        for (Object obj : args) {
            if (obj instanceof NativeNACheck<?>) {
                ((NativeNACheck<?>) obj).close();
            }
        }
    }
}
