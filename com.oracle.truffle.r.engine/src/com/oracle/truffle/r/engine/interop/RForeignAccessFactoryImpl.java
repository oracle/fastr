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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.r.ffi.impl.interop.FFI_RForeignAccessFactoryImpl;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.RForeignAccessFactory;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDouble;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RInteropScalar;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RTruffleObject;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;

/**
 * For most types we use the {@link MessageResolution} facility to automatically generate the
 * factory for creating the {@link ForeignAccess} instance. The exceptions are the (many) subclasses
 * of {@link RAbstractVector} as these have the same handling but the generator cannot handle
 * abstract classes.
 *
 * The types that must flow across the interop boundary fall into (at least) two categories. Those
 * listed in this class are those that other peer languages might expect to receive and make sense
 * of. There are also a large number that are involved in implementing the R Foreign Function
 * Interface, which are essentially private to the implementation. These are enumerated in
 * {@link FFI_RForeignAccessFactoryImpl}.
 */
public final class RForeignAccessFactoryImpl implements RForeignAccessFactory {

    @Override
    public ForeignAccess getForeignAccess(RTruffleObject obj) {
        CompilerAsserts.neverPartOfCompilation("getForeignAccess");
        if (obj instanceof RNull) {
            return RNullMRForeign.ACCESS;
        } else if (obj instanceof RList) {
            return RListMRForeign.ACCESS;
        } else if (obj instanceof RExpression) {
            return RExpressionMRForeign.ACCESS;
        } else if (obj instanceof REnvironment) {
            return REnvironmentMRForeign.ACCESS;
        } else if (obj instanceof RPairList) {
            return RPairListMRForeign.ACCESS;
        } else if (obj instanceof RFunction) {
            return RFunctionMRForeign.ACCESS;
        } else if (obj instanceof RSymbol) {
            return RSymbolMRForeign.ACCESS;
        } else if (obj instanceof RExternalPtr) {
            return RExternalPtrMRForeign.ACCESS;
        } else if (obj instanceof RUnboundValue) {
            return RUnboundValueMRForeign.ACCESS;
        } else if (obj instanceof RInteger) {
            return RIntegerMRForeign.ACCESS;
        } else if (obj instanceof RDouble) {
            return RDoubleMRForeign.ACCESS;
        } else if (obj instanceof RConnection) {
            return RConnectionMRForeign.ACCESS;
        } else if (obj instanceof RContext) {
            return RContextMRForeign.ACCESS;
        } else if (obj instanceof RS4Object) {
            return RS4ObjectMRForeign.ACCESS;
        } else if (obj instanceof RPromise) {
            return RPromiseMRForeign.ACCESS;
        } else if (obj instanceof RArgsValuesAndNames) {
            return RArgsValuesAndNamesMRForeign.ACCESS;
        } else if (obj instanceof RLanguage) {
            return RLanguageMRForeign.ACCESS;
        } else if (obj instanceof ActiveBinding) {
            return ActiveBindingMRForeign.ACCESS;
        } else if (obj instanceof RInteropScalar) {
            return RInteropScalarMRForeign.ACCESS;
        } else if (obj instanceof RMissing) {
            return RMissingMRForeign.ACCESS;
        } else if (obj instanceof REmpty) {
            return REmptyMRForeign.ACCESS;
        } else if (obj instanceof RAbstractAtomicVector) {
            return ForeignAccess.create(RAbstractAtomicVector.class, new RAbstractVectorAccessFactory());
        } else {
            ForeignAccess access = FFI_RForeignAccessFactoryImpl.getForeignAccess(obj);
            if (access != null) {
                return access;
            } else {
                throw RInternalError.unimplemented("missing foreign access factory for " + obj.getClass().getSimpleName());
            }
        }
    }

    @Override
    public boolean setIsNull(boolean value) {
        return RNullMR.setIsNull(value);
    }
}
