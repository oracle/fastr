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
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;

import java.util.logging.Level;

public class AltIntegerClassDescriptor extends AltVecClassDescriptor {
    // TODO: Fix signature (sint64?)
    private static final String eltMethodSignature = "(pointer, sint32):sint32";
    private static final String getRegionMethodSignature = "(pointer, sint32, sint32, [sint32]):sint32";
    private static final String sumMethodSignature = "(pointer, sint32):pointer";
    private static final String minMethodSignature = "(pointer, sint32):pointer";
    private static final String maxMethodSignature = "(pointer, sint32):pointer";
    private static final String isSortedMethodSignature = "(pointer):sint32";
    private static final int eltMethodArgCount = 2;
    private static final int getRegionMethodArgCount = 4;
    private static final int sumMethodArgCount = 2;
    private static final int minMethodArgCount = 2;
    private static final int maxMethodArgCount = 2;
    private static final int isSortedMethodArgCount = 1;
    private Object eltMethod;
    private Object getRegionMethod;
    private Object isSortedMethod;
    private Object noNAMethod;
    private Object sumMethod;
    private Object maxMethod;
    private Object minMethod;
    private static final TruffleLogger logger = RLogger.getLogger("altrep");

    public AltIntegerClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public Object getEltMethod() {
        assert eltMethod != null;
        return eltMethod;
    }

    public Object getGetRegionMethod() {
        return getRegionMethod;
    }

    public Object getIsSortedMethod() {
        return isSortedMethod;
    }

    public Object getNoNAMethod() {
        return noNAMethod;
    }

    public Object getSumMethod() {
        return sumMethod;
    }

    public Object getMaxMethod() {
        return maxMethod;
    }

    public Object getMinMethod() {
        return minMethod;
    }

    public String getEltMethodSignature() {
        return eltMethodSignature;
    }

    public void registerEltMethod(Object eltMethod) {
        logRegisterMethod("Elt");
        this.eltMethod = eltMethod;
    }

    public void registerGetRegionMethod(Object getRegionMethod) {
        logRegisterMethod("Get_region");
        this.getRegionMethod = getRegionMethod;
    }

    public void registerIsSortedMethod(Object isSortedMethod) {
        logRegisterMethod("Is_sorted");
        this.isSortedMethod = isSortedMethod;
    }

    public void registerNoNAMethod(Object noNAMethod) {
        logRegisterMethod("No_NA");
        this.noNAMethod = noNAMethod;
    }

    public void registerSumMethod(Object sumMethod) {
        logRegisterMethod("Sum");
        this.sumMethod = sumMethod;
    }

    public void registerMaxMethod(Object maxMethod) {
        logRegisterMethod("Max");
        this.maxMethod = maxMethod;
    }

    public void registerMinMethod(Object minMethod) {
        logRegisterMethod("Min");
        this.minMethod = minMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethod != null;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethod != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethod != null;
    }

    public boolean isSumMethodRegistered() {
        return sumMethod != null;
    }

    public boolean isMaxMethodRegistered() {
        return maxMethod != null;
    }

    public boolean isMinMethodRegistered() {
        return minMethod != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethod != null;
    }

    @Override
    public String toString() {
        return "ALTINT class descriptor for " + super.toString();
    }

    public int invokeEltMethodUncached(Object instance, int index) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(eltMethod);
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        return invokeEltMethod(instance, index, methodInterop, hasMirrorProfile);
    }

    public int invokeEltMethodCached(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        return invokeEltMethod(instance, index, eltMethodInterop, hasMirrorProfile);
    }

    public long invokeGetRegionMethodCached(Object instance, long fromIdx, long size, Object buffer, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        return invokeGetRegionMethod(instance, fromIdx, size, buffer, methodInterop, hasMirrorProfile);
    }

    public Object invokeSumMethodCached(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        return invokeSumMethod(instance, naRm, methodInterop, hasMirrorProfile);
    }

    public Object invokeMinMethodCached(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        return invokeMinMethod(instance, naRm, methodInterop, hasMirrorProfile);
    }

    public Object invokeMaxMethodCached(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        return invokeMaxMethod(instance, naRm, methodInterop, hasMirrorProfile);
    }

    public int invokeIsSortedMethodCached(Object instance, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        return invokeIsSortedMethod(instance, methodInterop, hasMirrorProfile);
    }

    private int invokeEltMethod(Object instance, int index, InteropLibrary eltMethodInterop, ConditionProfile hasMirrorProfile) {
        assert eltMethod != null;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("elt", instance, index);
        }
        Object element = invokeNativeFunction(eltMethodInterop, eltMethod, eltMethodSignature, eltMethodArgCount, hasMirrorProfile, instance, index);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(element);
        }
        assert element instanceof Integer;
        return (int) element;
    }

    private long invokeGetRegionMethod(Object instance, long fromIdx, long size, Object buffer, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        assert getRegionMethod != null;
        if (buffer instanceof int[]) {
            throw RInternalError.shouldNotReachHere("Calls from managed code are unimplemented");
        }
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("GetRegion", instance, fromIdx, size, buffer);
        }
        Object copiedCount = invokeNativeFunction(methodInterop, getRegionMethod, getRegionMethodSignature, getRegionMethodArgCount, hasMirrorProfile, instance, fromIdx, size, buffer);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(copiedCount);
        }
        assert copiedCount instanceof Long;
        return (long) copiedCount;
    }

    private Object invokeSumMethod(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Sum", instance, naRm);
        }
        Object sumVectorMirror = invokeNativeFunction(methodInterop, sumMethod, sumMethodSignature, sumMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(sumVectorMirror);
    }

    private Object invokeMinMethod(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Min", instance, naRm);
        }
        Object minVectorMirror = invokeNativeFunction(methodInterop, minMethod, minMethodSignature, minMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(minVectorMirror);
    }

    private Object invokeMaxMethod(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Max", instance, naRm);
        }
        Object maxVectorMirror = invokeNativeFunction(methodInterop, maxMethod, maxMethodSignature, maxMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(maxVectorMirror);
    }

    private int invokeIsSortedMethod(Object instance, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Is_sorted", instance);
        }
        Object sortedMode = invokeNativeFunction(methodInterop, isSortedMethod, isSortedMethodSignature, isSortedMethodArgCount, hasMirrorProfile, instance);
        assert sortedMode instanceof Integer;
        return (int) sortedMode;
    }

    private Object convertNativeReturnValToIntOrDouble(Object returnValueFromNative) {
        assert returnValueFromNative instanceof NativeDataAccess.NativeMirror;
        RBaseObject returnValue = ((NativeDataAccess.NativeMirror) returnValueFromNative).getDelegate();
        assert returnValue instanceof RIntVector || returnValue instanceof RDoubleVector;
        if (returnValue instanceof RIntVector) {
            return ((RIntVector) returnValue).getDataAt(0);
        } else if (returnValue instanceof RDoubleVector) {
            return ((RDoubleVector) returnValue).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

}
