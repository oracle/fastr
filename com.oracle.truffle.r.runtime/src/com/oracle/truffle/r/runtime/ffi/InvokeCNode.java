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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.CRFFIUnwrapVectorNode.CRFFIUnwrapVectorsNode;
import com.oracle.truffle.r.runtime.ffi.CRFFIUnwrapVectorNodeGen.CRFFIUnwrapVectorsNodeGen;
import com.oracle.truffle.r.runtime.ffi.CRFFIWrapVectorNode.CRFFIWrapVectorsNode;
import com.oracle.truffle.r.runtime.ffi.CRFFIWrapVectorNodeGen.CRFFIWrapVectorsNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ReportPolymorphism
public abstract class InvokeCNode extends RBaseNode {

    @Child private CRFFIWrapVectorsNode argsWrapperNode = CRFFIWrapVectorsNodeGen.create();
    @Child private CRFFIUnwrapVectorsNode argsUnwrapperNode = CRFFIUnwrapVectorsNodeGen.create();
    @Child private FunctionObjectGetter functionGetterNode;

    public InvokeCNode(FunctionObjectGetter functionGetterNode) {
        this.functionGetterNode = functionGetterNode;
    }

    /**
     * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
     * {@code args}. The values in {@code args} should support the IS_POINTER/AS_POINTER messages.
     */
    protected abstract void execute(NativeCallInfo nativeCallInfo, Object[] args);

    public final RList dispatch(NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args) {
        @SuppressWarnings("unused")
        boolean dupArgs = RRuntime.fromLogical(dup);
        @SuppressWarnings("unused")
        boolean checkNA = RRuntime.fromLogical(naok);

        Object[] preparedArgs = argsWrapperNode.execute(args.getArguments());

        RFFIContext stateRFFI = RContext.getInstance().getStateRFFI();
        long before = stateRFFI.beforeDowncall();
        try {
            execute(nativeCallInfo, preparedArgs);
            return RDataFactory.createList(argsUnwrapperNode.execute(preparedArgs), validateArgNames(preparedArgs.length, args.getSignature()));
        } finally {
            stateRFFI.afterDowncall(before);
        }
    }

    protected final TruffleObject getFunction(TruffleObject address, int arity) {
        return functionGetterNode.execute(address, arity);
    }

    public static Node createExecute() {
        return Message.EXECUTE.createNode();
    }

    @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
    protected void invokeCallCached(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, Object[] args,
                    @SuppressWarnings("unused") @Cached("args.length") int cachedArgsLength,
                    @Cached("createExecute()") Node executeNode,
                    @SuppressWarnings("unused") @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                    @Cached("getFunction(cachedAddress, cachedArgsLength)") TruffleObject cachedFunction) {
        try {
            ForeignAccess.sendExecute(executeNode, cachedFunction, args);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(replaces = "invokeCallCached", limit = "99", guards = "args.length == cachedArgsLength")
    protected void invokeCallCachedLength(NativeCallInfo nativeCallInfo, Object[] args,
                    @Cached("args.length") int cachedArgsLength,
                    @Cached("createExecute()") Node executeNode) {
        try {
            ForeignAccess.sendExecute(executeNode, getFunction(nativeCallInfo.address.asTruffleObject(), cachedArgsLength), args);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private static RStringVector validateArgNames(int argsLength, ArgumentsSignature signature) {
        String[] listArgNames = new String[argsLength];
        for (int i = 0; i < argsLength; i++) {
            String name = signature.getName(i);
            if (name == null) {
                name = RRuntime.NAMES_ATTR_EMPTY_VALUE;
            }
            listArgNames[i] = name;
        }
        return RDataFactory.createStringVector(listArgNames, RDataFactory.COMPLETE_VECTOR);
    }

    public abstract static class FunctionObjectGetter extends Node {

        public abstract TruffleObject execute(TruffleObject address, int arity);

    }

}
