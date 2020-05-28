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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;

import java.util.Arrays;
import java.util.logging.Level;

public abstract class AltRepClassDescriptor extends RBaseObject {
    private final int lengthMethodArgCount = 1;
    private final int duplicateMethodArgCount = 2;
    public static final String unserializeMethodSignature = "(pointer, pointer): pointer";
    public static final String unserializeEXMethodSignature = "(pointer, pointer, pointer, sint32, sint32): pointer";
    public static final String serializedStateMethodSignature = "(pointer): pointer";
    public static final String duplicateMethodSignature = "(pointer, sint32): pointer";
    public static final String duplicateEXMethodSignature = "(pointer, sint32): pointer";
    public static final String coerceMethodSignature = "(pointer, sint32): pointer";
    public static final String inspectMethodSignature = "(pointer, sint32, sint32, sint32, (pointer,sint32,sint32,sint32):void): sint32";
    public static final String lengthMethodSignature = "(pointer): sint32";
    // Instance data
    private final String className;
    private final String packageName;
    private final Object dllInfo;
    // Methods
    private AltrepDownCall unserializeDownCall;
    private AltrepDownCall unserializeEXDownCall;
    private AltrepDownCall serializedStateDownCall;
    private AltrepDownCall duplicateDownCall;
    private AltrepDownCall duplicateEXDownCall;
    private AltrepDownCall coerceDownCall;
    private AltrepDownCall inspectDownCall;
    private AltrepDownCall lengthDownCall;
    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_ALTREP);

    AltRepClassDescriptor(String className, String packageName, Object dllInfo) {
        this.className = className;
        this.packageName = packageName;
        this.dllInfo = dllInfo;
    }

    @Override
    public RType getRType() {
        // TODO: R_altrep_class_t native type is a wrapper for SEXP which is a pairlist.
        return RType.PairList;
    }

    public AltrepDownCall getDuplicateDownCall() {
        return duplicateDownCall;
    }

    public AltrepDownCall getLengthDownCall() {
        return lengthDownCall;
    }

    protected void logRegisterMethod(String methodName) {
        logger.finer(() -> "Register " + methodName + " on " + toString());
    }

    public void registerUnserializeMethod(AltrepDownCall downCall) {
        logRegisterMethod("Unserialize");
        this.unserializeDownCall = downCall;
    }

    public void registerUnserializeEXMethod(AltrepDownCall downCall) {
        logRegisterMethod("Unserialize_EX");
        this.unserializeEXDownCall = downCall;
    }

    public void registerSerializedStateMethod(AltrepDownCall downCall) {
        logRegisterMethod("Serialized_state");
        this.serializedStateDownCall = downCall;
    }

    public void registerDuplicateMethod(AltrepDownCall downCall) {
        logRegisterMethod("Duplicate");
        this.duplicateDownCall = downCall;
    }

    public void registerDuplicateEXMethod(AltrepDownCall downCall) {
        logRegisterMethod("Duplicate_EX");
        this.duplicateEXDownCall = downCall;
    }

    public void registerCoerceMethod(AltrepDownCall downCall) {
        logRegisterMethod("Coerce");
        this.coerceDownCall = downCall;
    }

    public void registerInspectMethod(AltrepDownCall downCall) {
        logRegisterMethod("Inspect");
        this.inspectDownCall = downCall;
    }

    public void registerLengthMethod(AltrepDownCall lengthDownCall) {
        logRegisterMethod("Length");
        this.lengthDownCall = lengthDownCall;
    }

    public boolean isUnserializeMethodRegistered() {
        return unserializeDownCall != null;
    }

    public boolean isUnserializeEXMethodRegistered() {
        return unserializeEXDownCall != null;
    }

    public boolean isSerializedStateMethodRegistered() {
        return serializedStateDownCall != null;
    }

    public boolean isDuplicateMethodRegistered() {
        return duplicateDownCall != null;
    }

    public boolean isDuplicateEXMethodRegistered() {
        return duplicateEXDownCall != null;
    }

    public boolean isCoerceMethodRegistered() {
        return coerceDownCall != null;
    }

    public boolean isInspectMethodRegistered() {
        return inspectDownCall != null;
    }

    public boolean isLengthMethodRegistered() {
        return lengthDownCall != null;
    }

    @CompilerDirectives.TruffleBoundary
    protected void logBeforeInteropExecute(String methodName, Object instance, Object... methodArgs) {
        // Note: Do not call instance.toString() ==> It may cause stack overflow.
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(() -> this.getClass().getSimpleName() + ": Invoking " + methodName
                    + " with args=" + Arrays.toString(methodArgs) + " (instance argument not included in this message).");
        }
    }

    @CompilerDirectives.TruffleBoundary
    protected void logAfterInteropExecute(Object returnedValue) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(() -> this.getClass().getSimpleName() + ": Got " + returnedValue + " from interop.execute");
        }
    }

    public int invokeLengthMethodCached(Object instance, InteropLibrary lengthMethodInterop, ConditionProfile hasMirrorProfile) {
        return invokeLengthMethod(instance, lengthMethodInterop, hasMirrorProfile);
    }

    public int invokeLengthMethodUncached(Object instance) {
        ConditionProfile hasMirrorProfile = ConditionProfile.getUncached();
        InteropLibrary lengthMethodInterop = InteropLibrary.getFactory().getUncached(lengthDownCall.method);
        return invokeLengthMethod(instance, lengthMethodInterop, hasMirrorProfile);
    }

    public Object invokeDuplicateMethodCached(Object instance, boolean deep, InteropLibrary interop, ConditionProfile hasMirrorProfile) {
        return invokeDuplicateMethod(instance, deep, interop, hasMirrorProfile);
    }

    /***
     *
     * @param instance Altrep instance. This is the "self" parameter that is passed to every altrep method.
     * @param args Rest of the arguments.
     */
    static Object invokeNativeFunction(InteropLibrary interop, Object function, String functionSignature, int argLen, ConditionProfile hasMirrorProfile, Object instance, Object... args) {
        assert instance instanceof RBaseObject;
        NativeDataAccess.NativeMirror mirror = wrapInNativeMirror((RBaseObject) instance, hasMirrorProfile);
        Object[] allArgs = collectArguments(mirror, argLen, args);
        try {
            if (interop.isMemberInvocable(function, "bind")) {
                // NFI case
                Object bound = interop.invokeMember(function, "bind", functionSignature);
                return interop.execute(bound, allArgs);
            } else {
                // LLVM case
                return interop.execute(function, allArgs);
            }
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere("Exception in invoke...Method");
        }
    }

    private static NativeDataAccess.NativeMirror wrapInNativeMirror(RBaseObject altrepInstance, ConditionProfile hasMirrorProfile) {
        NativeDataAccess.NativeMirror mirror = altrepInstance.getNativeMirror();
        if (hasMirrorProfile.profile(mirror == null)) {
            return NativeDataAccess.createNativeMirror(altrepInstance);
        } else {
            return mirror;
        }
    }

    private static Object[] collectArguments(Object firstArg, int argLen, Object... restOfArgs) {
        CompilerAsserts.compilationConstant(argLen);
        Object[] newArgs = new Object[argLen];
        newArgs[0] = firstArg;
        System.arraycopy(restOfArgs, 0, newArgs, 1, argLen - 1);
        return newArgs;
    }

    private Object invokeDuplicateMethod(Object instance, boolean deep, InteropLibrary interop, ConditionProfile hasMirrorProfile) {
        Object ret = invokeNativeFunction(interop, duplicateDownCall.method, duplicateMethodSignature, duplicateMethodArgCount, hasMirrorProfile, instance, deep);
        // TODO: Return type checks?
        return ret;
    }

    private int invokeLengthMethod(Object instance, InteropLibrary lengthMethodInterop, ConditionProfile hasMirrorProfile) {
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("lengthMethod", instance);
        }
        Object ret = invokeNativeFunction(lengthMethodInterop, lengthDownCall.method, lengthMethodSignature, lengthMethodArgCount, hasMirrorProfile, instance);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(ret);
        }
        assert ret instanceof Integer || ret instanceof Long;
        if (ret instanceof Long) {
            return ((Long) ret).intValue();
        } else {
            return (int) ret;
        }
    }

    @Override
    public String toString() {
        return packageName + ":" + className;
    }

}
