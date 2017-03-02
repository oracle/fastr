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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.MiscRFFI;

public class TruffleNFI_Misc implements MiscRFFI {

    private static class TruffleNFI_ExactSumNode extends ExactSumNode {
        @Child private Node messageNode = Message.createExecute(4).createNode();
        @CompilationFinal private TruffleObject function;

        @Override
        public double execute(double[] values, boolean hasNa, boolean naRm) {
            if (function == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                function = TruffleNFI_Utils.lookupAndBind("exactSumFunc", "([double], sint32, sint32, sint32): double");
            }
            try {
                return (double) ForeignAccess.sendExecute(messageNode, function, JavaInterop.asTruffleObject(values), values.length, hasNa ? 1 : 0, naRm ? 1 : 0);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @Override
    public ExactSumNode createExactSumNode() {
        return new TruffleNFI_ExactSumNode();
    }
}
