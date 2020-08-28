/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;
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
import com.oracle.truffle.r.runtime.ffi.DLLRFFI.LibHandle;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory.Type;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Invokes native C/Fortran function that takes primitive arrays as arguments, i.e., we do not pass
 * the R objects to such functions, and such functions are usually not up-calling back to FastR,
 * because they are plain C/Fortran code working on plain C/Fortran types, not SEXPs. However, in
 * practice we've seen that those functions 1) up-call back to R and 2) developers passing other
 * objects than atomic vectors and such objects are sent as SEXPs to the native code, which than
 * casts those to {@code SEXP} are then proceed as usual.
 *
 * The contract is that the native code can write to the arrays that are passed to it, so data of
 * non-temporary vectors must not be passed to the native code, instead they must be copied and data
 * field of the copy is passed to the native code. The calling R code wants to "read back" the
 * results and that is why this function returns {@link RList} with vectors corresponding to the
 * arguments (or their copies if they were non-temporary).
 *
 * This class works as follows: it materializes all the arguments and retrieves the corresponding
 * {@link RObjectDataPtr} instances that represent the "data" array of those vectors. Any other
 * {@link com.oracle.truffle.r.runtime.data.RBaseObject} instances are transferred to
 * {@code NativeMirror} so that they can be sent as {@code SEXP} to the native code. After the
 * native call finishes, we read back the vector instances from the {@link RObjectDataPtr}s.
 */
@ReportPolymorphism
public abstract class InvokeCNode extends RBaseNode {

    @Child private CRFFIWrapVectorsNode argsWrapperNode = CRFFIWrapVectorsNodeGen.create();
    @Child private CRFFIUnwrapVectorsNode argsUnwrapperNode = CRFFIUnwrapVectorsNodeGen.create();
    @Child private FunctionObjectGetter functionGetterNode;
    private final ValueProfile stateRFFIProfile = ValueProfile.createClassProfile();

    public InvokeCNode(FunctionObjectGetter functionGetterNode) {
        this.functionGetterNode = functionGetterNode;
    }

    /**
     * Invoke the native method identified by {@code symbolInfo} passing it the arguments in
     * {@code args}. The values in {@code args} should support the IS_POINTER/AS_POINTER messages.
     */
    protected abstract void execute(NativeCallInfo nativeCallInfo, Object[] args);

    public final RList dispatch(VirtualFrame frame, NativeCallInfo nativeCallInfo, byte naok, byte dup, RArgsValuesAndNames args, RContext rCtx) {
        @SuppressWarnings("unused")
        boolean dupArgs = RRuntime.fromLogical(dup);
        @SuppressWarnings("unused")
        boolean checkNA = RRuntime.fromLogical(naok);

        Object[] preparedArgs = argsWrapperNode.execute(args.getArguments());

        RFFIContext stateRFFI = stateRFFIProfile.profile(rCtx.getStateRFFI());
        LibHandle handle = nativeCallInfo.dllInfo == null ? null : nativeCallInfo.dllInfo.handle;
        Type rffiType = handle == null ? stateRFFI.getDefaultRFFIType() : handle.getRFFIType();
        Object before = stateRFFI.beforeDowncall(frame.materialize(), rffiType);
        try {
            execute(nativeCallInfo, preparedArgs);
            return RDataFactory.createList(argsUnwrapperNode.execute(preparedArgs), validateArgNames(preparedArgs.length, args.getSignature()));
        } finally {
            stateRFFI.afterDowncall(before, rffiType, AfterDownCallProfiles.getUncached());
        }
    }

    protected final TruffleObject getFunction(TruffleObject address, int arity, NativeCallInfo nativeCallInfo) {
        return functionGetterNode.execute(address, arity, nativeCallInfo);
    }

    @Specialization(guards = {"args.length == cachedArgsLength", "nativeCallInfo.address.asTruffleObject() == cachedAddress"})
    protected void invokeCallCached(@SuppressWarnings("unused") NativeCallInfo nativeCallInfo, Object[] args,
                    @SuppressWarnings("unused") @Cached("args.length") int cachedArgsLength,
                    @SuppressWarnings("unused") @Cached("nativeCallInfo.address.asTruffleObject()") TruffleObject cachedAddress,
                    @Cached("getFunction(cachedAddress, cachedArgsLength, nativeCallInfo)") TruffleObject cachedFunction,
                    @CachedLibrary("cachedFunction") InteropLibrary cachedInterop) {
        try {
            cachedInterop.execute(cachedFunction, args);
        } catch (InteropException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    @Specialization(replaces = "invokeCallCached", limit = "99", guards = "args.length == cachedArgsLength")
    protected void invokeCallCachedLength(NativeCallInfo nativeCallInfo, Object[] args,
                    @Cached("args.length") int cachedArgsLength,
                    @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary interop) {
        try {
            interop.execute(getFunction(nativeCallInfo.address.asTruffleObject(), cachedArgsLength, nativeCallInfo), args);
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

        public abstract TruffleObject execute(TruffleObject address, int arity, NativeCallInfo nativeCallInfo);

    }

}
