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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

import java.util.logging.Level;

public class AltIntegerClassDescriptor extends AltVecClassDescriptor {
    // TODO: Fix signature (sint64?)
    private static final String eltMethodSignature = "(pointer, sint32):sint32";
    private static final String getRegionMethodSignature = "(pointer, sint32, sint32, [sint32]):sint32";
    private static final String sumMethodSignature = "(pointer, sint32):pointer";
    private static final String minMethodSignature = "(pointer, sint32):pointer";
    private static final String maxMethodSignature = "(pointer, sint32):pointer";
    private static final String isSortedMethodSignature = "(pointer):sint32";
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

    public int invokeEltMethod(Object instance, InteropLibrary eltMethodInterop, int index) {
        assert eltMethod != null;
        eltMethodInterop = eltMethodInterop == null ? InteropLibrary.getFactory().getUncached(eltMethod) : eltMethodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("elt", instance, index);
        }
        Object element = invokeNativeFunction(eltMethodInterop, eltMethod, eltMethodSignature, instance, index);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(element);
        }
        assert element instanceof Integer;
        return (int) element;
    }

    public long invokeGetRegionMethod(Object instance, InteropLibrary methodInterop, long fromIdx, long size, Object buffer) {
        assert getRegionMethod != null;
        if (buffer instanceof int[]) {
            throw RInternalError.shouldNotReachHere("Calls from managed code are unimplemented");
        }
        methodInterop = methodInterop == null ? InteropLibrary.getFactory().getUncached(getRegionMethod) : methodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("GetRegion", instance, fromIdx, size, buffer);
        }
        Object copiedCount = invokeNativeFunction(methodInterop, getRegionMethod, getRegionMethodSignature, instance, fromIdx, size, buffer);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(copiedCount);
        }
        assert copiedCount instanceof Long;
        return (long) copiedCount;
    }

    public Object invokeSumMethod(Object instance, InteropLibrary methodInterop, boolean naRm) {
        methodInterop = methodInterop == null ? InteropLibrary.getFactory().getUncached(sumMethod) : methodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Sum", instance, naRm);
        }
        Object sumVectorMirror = invokeNativeFunction(methodInterop, sumMethod, sumMethodSignature, instance, naRm);
        return convertNativeReturnValToIntOrDouble(sumVectorMirror);
    }

    public Object invokeMinMethod(Object instance, InteropLibrary methodInterop, boolean naRm) {
        methodInterop = methodInterop == null ? InteropLibrary.getFactory().getUncached(minMethod) : methodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Min", instance, naRm);
        }
        Object minVectorMirror = invokeNativeFunction(methodInterop, minMethod, minMethodSignature, instance, naRm);
        return convertNativeReturnValToIntOrDouble(minVectorMirror);
    }

    public Object invokeMaxMethod(Object instance, InteropLibrary methodInterop, boolean naRm) {
        methodInterop = methodInterop == null ? InteropLibrary.getFactory().getUncached(maxMethod) : methodInterop;
        logBeforeInteropExecute("Max", instance, naRm);
        Object maxVectorMirror = invokeNativeFunction(methodInterop, maxMethod, maxMethodSignature, instance, naRm);
        return convertNativeReturnValToIntOrDouble(maxVectorMirror);
    }

    public int invokeIsSortedMethod(Object instance, InteropLibrary methodInterop) {
        methodInterop = methodInterop == null ? InteropLibrary.getFactory().getUncached(isSortedMethod) : methodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("Is_sorted", instance);
        }
        Object sortedMode = invokeNativeFunction(methodInterop, isSortedMethod, isSortedMethodSignature, instance);
        assert sortedMode instanceof Integer;
        return (int) sortedMode;
    }

    private Object convertNativeReturnValToIntOrDouble(Object returnValueFromNative) {
        assert returnValueFromNative instanceof NativeDataAccess.NativeMirror;
        RBaseObject returnValue = ((NativeDataAccess.NativeMirror) returnValueFromNative).get();
        assert returnValue instanceof RAbstractIntVector || returnValue instanceof RAbstractDoubleVector;
        if (returnValue instanceof RAbstractIntVector) {
            return ((RAbstractIntVector) returnValue).getDataAt(0);
        } else if (returnValue instanceof RAbstractDoubleVector) {
            return ((RAbstractDoubleVector) returnValue).getDataAt(0);
        } else {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public String toString() {
        return "ALTINT class descriptor for " + super.toString();
    }

}
