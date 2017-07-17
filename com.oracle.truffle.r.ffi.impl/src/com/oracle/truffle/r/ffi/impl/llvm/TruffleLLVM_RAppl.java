/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.ffi.impl.interop.NativeDoubleArray;
import com.oracle.truffle.r.ffi.impl.interop.NativeIntegerArray;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.DLL;
import com.oracle.truffle.r.runtime.ffi.DLL.SymbolHandle;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

/**
 * See the comments in {@link TruffleLLVM_Lapack} regarding argument passing.
 */
public class TruffleLLVM_RAppl implements RApplRFFI {

    private static class TruffleLLVM_Dqrdc2Node extends Dqrdc2Node {
        @Child private Node message = LLVMFunction.dqrdc2.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
            NativeDoubleArray xN = new NativeDoubleArray(x);
            NativeIntegerArray rankN = new NativeIntegerArray(rank);
            NativeDoubleArray qrauxN = new NativeDoubleArray(qraux);
            NativeIntegerArray pivotN = new NativeIntegerArray(pivot);
            NativeDoubleArray workN = new NativeDoubleArray(work);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.dqrdc2.callName, null);
                }
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), xN, ldx, n, p, tol, rankN, qrauxN, pivotN, workN);
                // sync up in case copied to native memory
                xN.getValue();
                rankN.getValue();
                qrauxN.getValue();
                pivotN.getValue();
                workN.getValue();
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static class TruffleLLVM_DqrcfNode extends DqrcfNode {
        @Child private Node message = LLVMFunction.dqrcf.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
            NativeDoubleArray xN = new NativeDoubleArray(x);
            NativeDoubleArray qrauxN = new NativeDoubleArray(qraux);
            NativeDoubleArray yN = new NativeDoubleArray(y);
            NativeDoubleArray bN = new NativeDoubleArray(b);
            NativeIntegerArray infoN = new NativeIntegerArray(info);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.dqrcf.callName, null);
                }
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), xN, n, k, qrauxN, yN, ny, bN, infoN);
                // sync up in case copied to native memory
                xN.getValue();
                qrauxN.getValue();
                yN.getValue();
                bN.getValue();
                infoN.getValue();
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    private static class TruffleLLVM_DqrlsNode extends DqrlsNode {
        @Child private Node message = LLVMFunction.dqrls.createMessage();
        @CompilationFinal private SymbolHandle symbolHandle;

        @Override
        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            NativeDoubleArray xN = new NativeDoubleArray(x);
            NativeDoubleArray yN = new NativeDoubleArray(y);
            NativeDoubleArray bN = new NativeDoubleArray(b);
            NativeDoubleArray rsdN = new NativeDoubleArray(rsd);
            NativeDoubleArray qtyN = new NativeDoubleArray(qty);
            NativeIntegerArray kN = new NativeIntegerArray(k);
            NativeIntegerArray jpvtN = new NativeIntegerArray(jpvt);
            NativeDoubleArray qrauxN = new NativeDoubleArray(qraux);
            NativeDoubleArray workN = new NativeDoubleArray(work);
            try {
                if (symbolHandle == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    symbolHandle = DLL.findSymbol(LLVMFunction.dqrls.callName, null);
                }
                ForeignAccess.sendExecute(message, symbolHandle.asTruffleObject(), xN, n, p, yN, ny, tol, bN, rsdN, qtyN, kN, jpvtN, qrauxN, workN);
                // sync up in case copied to native memory
                xN.getValue();
                yN.getValue();
                bN.getValue();
                rsdN.getValue();
                qtyN.getValue();
                kN.getValue();
                jpvtN.getValue();
                qrauxN.getValue();
                workN.getValue();
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere(ex);
            }
        }
    }

    @Override
    public Dqrdc2Node createDqrdc2Node() {
        return new TruffleLLVM_Dqrdc2Node();
    }

    @Override
    public DqrcfNode createDqrcfNode() {
        return new TruffleLLVM_DqrcfNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new TruffleLLVM_DqrlsNode();
    }

    @Override
    public DqrqtyNode createDqrqtyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrqyNode createDqrqyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrrsdNode createDqrrsdNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrxbNode createDqrxbNode() {
        throw RInternalError.unimplemented();
    }
}
