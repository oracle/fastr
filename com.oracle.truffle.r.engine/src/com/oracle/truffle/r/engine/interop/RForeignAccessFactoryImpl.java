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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.engine.TruffleRLanguage;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RForeignAccessFactory;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.ffi.DLL;

/**
 * A {@link ForeignAccess} instance captures the {@link Thread} that creates it and all uses are
 * checked against the current thread. Therefore, in a world with multiple {@link PolyglotEngine}s,
 * aka multiple {@link Thread} and {@link RContext} instances, it is not possible to use a simple
 * global constant value for the {@link ForeignAccess} instance that could be associated directly
 * with the {@link TruffleObject} class.
 *
 * This factory provides a generic solution for all FastR types (all of which are
 * {@link TruffleObject}s), at some cost in performance.
 *
 * For most types we use the {@link MessageResolution} facility to automatically generate the
 * factory for creating the {@link ForeignAccess} instance. The exceptions are the (many) subclasses
 * of {@link RAbstractVector} as these have the same handling but the generator cannot handle
 * abstract classes.
 *
 */
public final class RForeignAccessFactoryImpl implements RForeignAccessFactory {
    private static final class TableEntry {
        private final Class<? extends RTruffleObject> clazz; // for sanity check
        private final ForeignAccess foreignAccess;
        /**
         * {@link PolyglotEngine} checks the thread on a {@link ForeignAccess}.
         */
        private final Thread thread;

        private TableEntry(Class<? extends RTruffleObject> clazz, ForeignAccess foreignAccess) {
            this.clazz = clazz;
            this.foreignAccess = foreignAccess;
            this.thread = Thread.currentThread();
        }
    }

    private final TableEntry[] table = new TableEntry[32];
    int tableIndex;

    private synchronized ForeignAccess get(RTruffleObject obj) {
        Class<? extends RTruffleObject> objclazz = obj.getClass();
        Thread thread = Thread.currentThread();
        for (int i = 0; i < tableIndex; i++) {
            TableEntry te = table[i];
            if (te.clazz == objclazz && te.thread == thread) {
                return te.foreignAccess;
            }
        }
        return createForeignAccess(objclazz);
    }

    @TruffleBoundary
    private static ForeignAccess createForeignAccess(Class<? extends RTruffleObject> clazz) {
        ForeignAccess foreignAccess = null;
        String name = clazz.getSimpleName();
        if (RNull.class.isAssignableFrom(clazz)) {
            foreignAccess = RNullMRForeign.ACCESS;
        } else if (RList.class.isAssignableFrom(clazz)) {
            foreignAccess = RListMRForeign.ACCESS;
        } else if (REnvironment.class.isAssignableFrom(clazz)) {
            foreignAccess = REnvironmentMRForeign.ACCESS;
        } else if (RPairList.class.isAssignableFrom(clazz)) {
            foreignAccess = RPairListMRForeign.ACCESS;
        } else if (RFunction.class.isAssignableFrom(clazz)) {
            foreignAccess = RFunctionMRForeign.ACCESS;
        } else if (DLL.DLLInfo.class.isAssignableFrom(clazz)) {
            foreignAccess = DLLInfoMRForeign.ACCESS;
        } else if (DLL.DotSymbol.class.isAssignableFrom(clazz)) {
            foreignAccess = DLLDotSymbolMRForeign.ACCESS;
        } else if (RSymbol.class.isAssignableFrom(clazz)) {
            foreignAccess = RSymbolMRForeign.ACCESS;
        } else if (RExternalPtr.class.isAssignableFrom(clazz)) {
            foreignAccess = RExternalPtrMRForeign.ACCESS;
        } else if (RUnboundValue.class.isAssignableFrom(clazz)) {
            foreignAccess = RUnboundValueMRForeign.ACCESS;
        } else if (NativeRawArray.class.isAssignableFrom(clazz)) {
            foreignAccess = NativeRawArrayMRForeign.ACCESS;
        } else if (NativeLogicalArray.class.isAssignableFrom(clazz)) {
            foreignAccess = NativeLogicalArrayMRForeign.ACCESS;
        } else if (NativeCharArray.class.isAssignableFrom(clazz)) {
            foreignAccess = NativeCharArrayMRForeign.ACCESS;
        } else if (NativeDoubleArray.class.isAssignableFrom(clazz)) {
            foreignAccess = NativeDoubleArrayMRForeign.ACCESS;
        } else if (NativeIntegerArray.class.isAssignableFrom(clazz)) {
            foreignAccess = NativeIntegerArrayMRForeign.ACCESS;
        } else if (RInteger.class.isAssignableFrom(clazz)) {
            foreignAccess = RIntegerMRForeign.ACCESS;
        } else if (RDouble.class.isAssignableFrom(clazz)) {
            foreignAccess = RDoubleMRForeign.ACCESS;
        } else if (CharSXPWrapper.class.isAssignableFrom(clazz)) {
            foreignAccess = CharSXPWrapperMRForeign.ACCESS;
        } else {
            if (RAbstractVector.class.isAssignableFrom(clazz)) {
                foreignAccess = ForeignAccess.create(RAbstractVector.class, new RAbstractVectorAccessFactory());
            } else {
                throw RInternalError.unimplemented("foreignAccess: " + name);
            }
        }
        return foreignAccess;
    }

    @Override
    public ForeignAccess getForeignAccess(RTruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("getForeignAccess");
        return get(obj);
    }

    @Override
    public Class<? extends TruffleLanguage<RContext>> getTruffleLanguage() {
        return TruffleRLanguage.class;
    }
}
