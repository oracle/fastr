/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.engine.interop.ffi.nfi.TruffleNFI_Base;
import com.oracle.truffle.r.engine.interop.ffi.nfi.TruffleNFI_PCRE;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RForeignAccessFactory;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.DLL;

/**
 * For most types we use the {@link MessageResolution} facility to automatically generate the
 * factory for creating the {@link ForeignAccess} instance. The exceptions are the (many) subclasses
 * of {@link RAbstractVector} as these have the same handling but the generator cannot handle
 * abstract classes.
 */
public final class RForeignAccessFactoryImpl implements RForeignAccessFactory {

    @Override
    public ForeignAccess getForeignAccess(RTruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("getForeignAccess");
        if (obj instanceof RNull) {
            return RNullMRForeign.ACCESS;
        } else if (obj instanceof RList) {
            return RListMRForeign.ACCESS;
        } else if (obj instanceof REnvironment) {
            return REnvironmentMRForeign.ACCESS;
        } else if (obj instanceof RPairList) {
            return RPairListMRForeign.ACCESS;
        } else if (obj instanceof RFunction) {
            return RFunctionMRForeign.ACCESS;
        } else if (obj instanceof DLL.DLLInfo) {
            return DLLInfoMRForeign.ACCESS;
        } else if (obj instanceof DLL.DotSymbol) {
            return DLLDotSymbolMRForeign.ACCESS;
        } else if (obj instanceof RSymbol) {
            return RSymbolMRForeign.ACCESS;
        } else if (obj instanceof RExternalPtr) {
            return RExternalPtrMRForeign.ACCESS;
        } else if (obj instanceof RUnboundValue) {
            return RUnboundValueMRForeign.ACCESS;
        } else if (obj instanceof NativeRawArray) {
            return NativeRawArrayMRForeign.ACCESS;
        } else if (obj instanceof NativeLogicalArray) {
            return NativeLogicalArrayMRForeign.ACCESS;
        } else if (obj instanceof NativeCharArray) {
            return NativeCharArrayMRForeign.ACCESS;
        } else if (obj instanceof NativeDoubleArray) {
            return NativeDoubleArrayMRForeign.ACCESS;
        } else if (obj instanceof NativeIntegerArray) {
            return NativeIntegerArrayMRForeign.ACCESS;
        } else if (obj instanceof RInteger) {
            return RIntegerMRForeign.ACCESS;
        } else if (obj instanceof RDouble) {
            return RDoubleMRForeign.ACCESS;
        } else if (obj instanceof CharSXPWrapper) {
            return CharSXPWrapperMRForeign.ACCESS;
        } else if (obj instanceof RConnection) {
            return RConnectionMRForeign.ACCESS;
        } else if (obj instanceof TruffleNFI_Base.TruffleNFI_UnameNode.UnameUpCallImpl) {
            return UnameUpCallImplMRForeign.ACCESS;
        } else if (obj instanceof TruffleNFI_Base.TruffleNFI_ReadlinkNode.SetResultImpl) {
            return SetResultImplMRForeign.ACCESS;
        } else if (obj instanceof TruffleNFI_Base.TruffleNFI_GlobNode.GlobUpCallImpl) {
            return GlobUpCallImplMRForeign.ACCESS;
        } else if (obj instanceof TruffleNFI_PCRE.TruffleNFI_CompileNode.MakeResultImpl) {
            return MakeResultImplMRForeign.ACCESS;
        } else if (obj instanceof TruffleNFI_PCRE.TruffleNFI_GetCaptureNamesNode.CaptureNamesImpl) {
            return CaptureNamesImplMRForeign.ACCESS;
        } else if (obj instanceof RS4Object) {
            return RS4ObjectMRForeign.ACCESS;
        } else if (obj instanceof RPromise) {
            return RPromiseMRForeign.ACCESS;
        } else if (obj instanceof RArgsValuesAndNames) {
            return RArgsValuesAndNamesMRForeign.ACCESS;
        } else if (obj instanceof RLanguage) {
            return RLanguageMRForeign.ACCESS;

        } else {
            if (obj instanceof RAbstractVector) {
                return ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
            } else {
                throw RInternalError.unimplemented("missing foreign access factory for " + obj.getClass().getSimpleName());
            }
        }

    }

    @Override
    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }
}
