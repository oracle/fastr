/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.altrep;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;

import java.util.logging.Level;

public abstract class AltVecClassDescriptor extends AltRepClassDescriptor {
    public static final String dataptrMethodSignature = "(pointer, sint32) : pointer";
    public static final boolean[] dataptrMethodWrapArguments = new boolean[]{true, false};
    public static final boolean dataptrMethodUnwrapResult = false;

    public static final String dataptrOrNullMethodSignature = "(pointer) : pointer";
    public static final boolean[] dataptrOrNullMethodWrapArguments = new boolean[]{true};
    public static final boolean dataptrOrNullMethodUnwrapResult = false;

    public static final String extractSubsetMethodSignature = "(pointer, pointer, pointer) : pointer";
    public static final boolean[] extractSubsetMethodWrapArguments = new boolean[]{true, true, true};
    public static final boolean extractSubsetMethodUnwrapResult = true;

    private static final int dataptrMethodArgCount = 2;
    private AltrepMethodDescriptor dataptrMethodDescriptor;
    private AltrepMethodDescriptor dataptrOrMethodDescriptor;
    private AltrepMethodDescriptor extractSubsetMethodDescriptor;
    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    AltVecClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerDataptrMethod(AltrepMethodDescriptor dataptrMethod) {
        logRegisterMethod("Dataptr");
        this.dataptrMethodDescriptor = dataptrMethod;
    }

    public void registerDataptrOrNullMethod(AltrepMethodDescriptor dataptrOrNullMethod) {
        logRegisterMethod("Dataptr_or_null");
        this.dataptrOrMethodDescriptor = dataptrOrNullMethod;
    }

    public void registerExtractSubsetMethod(AltrepMethodDescriptor extractSubsetMethod) {
        logRegisterMethod("Extract_Subset");
        this.extractSubsetMethodDescriptor = extractSubsetMethod;
    }

    public AltrepMethodDescriptor getDataptrMethodDescriptor() {
        return dataptrMethodDescriptor;
    }

    public boolean isDataptrMethodRegistered() {
        return dataptrMethodDescriptor != null;
    }

    public boolean isDataptrOrNullMethodRegistered() {
        return dataptrOrMethodDescriptor != null;
    }

    public boolean isExtractSubsetMethodRegistered() {
        return extractSubsetMethodDescriptor != null;
    }

    public long invokeDataptrMethodCached(Object instance, boolean writeabble, InteropLibrary dataptrMethodInterop,
                                          InteropLibrary dataptrInterop, ConditionProfile hasMirrorProfile) {
        return invokeDataptrMethod(instance, writeabble, dataptrMethodInterop, dataptrInterop, hasMirrorProfile);
    }

    public long invokeDataptrMethodUncached(Object instance, boolean writeabble) {
        InteropLibrary dataptrMethodInterop = InteropLibrary.getFactory().getUncached(dataptrMethodDescriptor.method);
        InteropLibrary dataptrInterop = InteropLibrary.getFactory().getUncached();
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        return invokeDataptrMethod(instance, writeabble, dataptrMethodInterop, dataptrInterop, hasMirrorProfile);
    }

    // TODO: Bude vracet truffleobject co ma hasArrayElements
    private long invokeDataptrMethod(Object instance, boolean writeabble, InteropLibrary dataptrMethodInterop, InteropLibrary dataptrInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("dataptr", instance, writeabble);
        }
        Object dataptr = invokeNativeFunction(dataptrMethodInterop, dataptrMethodDescriptor.method, dataptrMethodSignature, dataptrMethodArgCount, hasMirrorProfile, instance, writeabble); // TODO
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(dataptr);
        }
        try {
            // TODO: return DataptrWrapper(dataptr);
            if (!dataptrInterop.isPointer(dataptr)) {
                // TODO: Volat tohle na VectorRFFIWrapperu je strasne drahy.
                dataptrInterop.toNative(dataptr);
            }
            return dataptrInterop.asPointer(dataptr);
        } catch (Exception e) {
            throw RInternalError.shouldNotReachHere(e, "Exception in invokeDataptrMethod");
        }
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
