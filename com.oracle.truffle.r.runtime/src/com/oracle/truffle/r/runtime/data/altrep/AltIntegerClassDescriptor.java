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
    public static final String eltMethodSignature = "(pointer, sint32):sint32";
    public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = false;

    public static final String getRegionMethodSignature = "(pointer, sint32, sint32, [sint32]):sint32";
    public static final boolean[] getRegionMethodWrapArguments = new boolean[]{true, false, false, false};
    public static final boolean getRegionMethodUnwrapResult = false;

    public static final String isSortedMethodSignature = "(pointer):sint32";
    public static final boolean[] isSortedMethodWrapArguments = new boolean[]{true};
    public static final boolean isSortedMethodUnwrapResult = false;

    public static final String noNAMethodSignature = "(pointer):sint32";
    public static final boolean[] noNAMethodWrapArguments = new boolean[]{true};
    public static final boolean noNAMethodUnwrapResult = false;

    public static final String sumMethodSignature = "(pointer, sint32):pointer";
    public static final boolean[] sumMethodWrapArguments = new boolean[]{true, false};
    public static final boolean sumMethodUnwrapResult = true;

    public static final String minMethodSignature = "(pointer, sint32):pointer";
    public static final boolean[] minMethodWrapArguments = new boolean[]{true, false};
    public static final boolean minMethodUnwrapResult = true;

    public static final String maxMethodSignature = "(pointer, sint32):pointer";
    public static final boolean[] maxMethodWrapArguments = new boolean[]{true, false};
    public static final boolean maxMethodUnwrapResult = true;

    private static final int eltMethodArgCount = 2;
    private static final int getRegionMethodArgCount = 4;
    private static final int sumMethodArgCount = 2;
    private static final int minMethodArgCount = 2;
    private static final int maxMethodArgCount = 2;
    private static final int isSortedMethodArgCount = 1;
    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;
    private AltrepMethodDescriptor sumMethodDescriptor;
    private AltrepMethodDescriptor minMethodDescriptor;
    private AltrepMethodDescriptor maxMethodDescriptor;
    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    public AltIntegerClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        assert eltMethodDescriptor != null;
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getGetRegionMethodDescriptor() {
        return getRegionMethodDescriptor;
    }

    public AltrepMethodDescriptor getIsSortedMethodDescriptor() {
        return isSortedMethodDescriptor;
    }

    public AltrepMethodDescriptor getNoNAMethodDescriptor() {
        return noNAMethodDescriptor;
    }

    public AltrepMethodDescriptor getSumMethodDescriptor() {
        return sumMethodDescriptor;
    }

    public AltrepMethodDescriptor getMaxMethodDescriptor() {
        return maxMethodDescriptor;
    }

    public AltrepMethodDescriptor getMinMethodDescriptor() {
        return minMethodDescriptor;
    }

    public void registerEltMethod(AltrepMethodDescriptor eltMethod) {
        logRegisterMethod("Elt");
        this.eltMethodDescriptor = eltMethod;
    }

    public void registerGetRegionMethod(AltrepMethodDescriptor getRegionMethod) {
        logRegisterMethod("Get_region");
        this.getRegionMethodDescriptor = getRegionMethod;
    }

    public void registerIsSortedMethod(AltrepMethodDescriptor isSortedMethod) {
        logRegisterMethod("Is_sorted");
        this.isSortedMethodDescriptor = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepMethodDescriptor noNAMethod) {
        logRegisterMethod("No_NA");
        this.noNAMethodDescriptor = noNAMethod;
    }

    public void registerSumMethod(AltrepMethodDescriptor sumMethod) {
        logRegisterMethod("Sum");
        this.sumMethodDescriptor = sumMethod;
    }

    public void registerMaxMethod(AltrepMethodDescriptor maxMethod) {
        logRegisterMethod("Max");
        this.maxMethodDescriptor = maxMethod;
    }

    public void registerMinMethod(AltrepMethodDescriptor minMethod) {
        logRegisterMethod("Min");
        this.minMethodDescriptor = minMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethodDescriptor != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethodDescriptor != null;
    }

    public boolean isSumMethodRegistered() {
        return sumMethodDescriptor != null;
    }

    public boolean isMaxMethodRegistered() {
        return maxMethodDescriptor != null;
    }

    public boolean isMinMethodRegistered() {
        return minMethodDescriptor != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethodDescriptor != null;
    }

    @Override
    public String toString() {
        return "ALTINT class descriptor for " + super.toString();
    }

    public int invokeEltMethodUncached(Object instance, int index) {
        InteropLibrary methodInterop = InteropLibrary.getFactory().getUncached(eltMethodDescriptor.method);
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
        assert eltMethodDescriptor.method != null;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("elt", instance, index);
        }
        Object element = invokeNativeFunction(eltMethodInterop, eltMethodDescriptor.method, eltMethodSignature, eltMethodArgCount, hasMirrorProfile, instance, index);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(element);
        }
        assert element instanceof Integer;
        return (int) element;
    }

    private long invokeGetRegionMethod(Object instance, long fromIdx, long size, Object buffer, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        assert getRegionMethodDescriptor.method != null;
        if (buffer instanceof int[]) {
            throw RInternalError.shouldNotReachHere("Calls from managed code are unimplemented");
        }
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("GetRegion", instance, fromIdx, size, buffer);
        }
        Object copiedCount = invokeNativeFunction(methodInterop, getRegionMethodDescriptor.method, getRegionMethodSignature, getRegionMethodArgCount, hasMirrorProfile, instance, fromIdx, size, buffer);
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
        Object sumVectorMirror = invokeNativeFunction(methodInterop, sumMethodDescriptor.method, sumMethodSignature, sumMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(sumVectorMirror);
    }

    private Object invokeMinMethod(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Min", instance, naRm);
        }
        Object minVectorMirror = invokeNativeFunction(methodInterop, minMethodDescriptor.method, minMethodSignature, minMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(minVectorMirror);
    }

    private Object invokeMaxMethod(Object instance, boolean naRm, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Max", instance, naRm);
        }
        Object maxVectorMirror = invokeNativeFunction(methodInterop, maxMethodDescriptor.method, maxMethodSignature, maxMethodArgCount, hasMirrorProfile, instance, naRm);
        return convertNativeReturnValToIntOrDouble(maxVectorMirror);
    }

    private int invokeIsSortedMethod(Object instance, InteropLibrary methodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Is_sorted", instance);
        }
        Object sortedMode = invokeNativeFunction(methodInterop, isSortedMethodDescriptor.method, isSortedMethodSignature, isSortedMethodArgCount, hasMirrorProfile, instance);
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
