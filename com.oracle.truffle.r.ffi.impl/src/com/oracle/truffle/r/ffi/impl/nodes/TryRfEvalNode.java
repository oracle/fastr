/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.util.WritePointerNode;

public final class TryRfEvalNode extends FFIUpCallNode.Arg4 {
    @Child private RfEvalNode rfEvalNode = RfEvalNode.create();
    @Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());
    @Child private WritePointerNode writePointerNode;

    @Override
    public Object executeObject(Object expr, Object env, Object errorFlag, Object silent) {
        Object handlerStack = RErrorHandling.getHandlerStack();
        Object restartStack = RErrorHandling.getRestartStack();
        boolean ok = true;
        Object result = RNull.instance;
        try {
            // TODO handle silent
            RErrorHandling.resetStacks();
            result = rfEvalNode.executeObject(expr, env);
        } catch (Throwable t) {
            ok = false;
            result = RNull.instance;
        } finally {
            RErrorHandling.restoreStacks(handlerStack, restartStack);
        }
        if (!interop.isNull(errorFlag)) {
            writeErrorFlag(errorFlag, ok);
        }
        return result;
    }

    public void writeErrorFlag(Object errorFlag, boolean ok) {
        if (writePointerNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            writePointerNode = insert(WritePointerNode.create());
        }
        writePointerNode.execute(errorFlag, 0, ok ? 0 : 1);
    }
}
