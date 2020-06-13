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
    public static final String unserializeMethodSignature = "(pointer, pointer): pointer";
    public static final boolean[] unserializeMethodWrapArguments = new boolean[]{true, true};
    public static final boolean unserializeMethodUnwrapResult = true;

    public static final String unserializeEXMethodSignature = "(pointer, pointer, pointer, sint32, sint32): pointer";
    public static final boolean[] unserializeEXMethodWrapArguments = new boolean[]{true, true, true, false, false};
    public static final boolean unserializeEXMethodUnwrapResult = true;

    public static final String serializedStateMethodSignature = "(pointer): pointer";
    public static final boolean[] serializedStateMethodWrapArguments = new boolean[]{true};
    public static final boolean serializedStateMethodUnwrapResult = true;

    public static final String duplicateMethodSignature = "(pointer, sint32): pointer";
    public static final boolean[] duplicateMethodWrapArguments = new boolean[]{true, false};
    public static final boolean duplicateMethodUnwrapResult = true;

    public static final String duplicateEXMethodSignature = "(pointer, sint32): pointer";
    public static final boolean[] duplicateEXMethodWrapArguments = new boolean[]{true, false};
    public static final boolean duplicateEXMethodUnwrapResult = true;

    public static final String coerceMethodSignature = "(pointer, sint32): pointer";
    public static final boolean[] coerceMethodWrapArguments = new boolean[]{true, false};
    public static final boolean coerceMethodUnwrapResult = true;

    public static final String inspectMethodSignature = "(pointer, sint32, sint32, sint32, (pointer,sint32,sint32,sint32):void): sint32";
    public static final boolean[] inspectMethodWrapArguments = new boolean[]{true, false, false, false, false};
    public static final boolean inspectMethodUnwrapResult = false;

    public static final String lengthMethodSignature = "(pointer): sint32";
    public static final boolean[] lengthMethodWrapArguments = new boolean[]{true};
    public static final boolean lengthMethodUnwrapResult = false;

    // Instance data
    private final String className;
    private final String packageName;
    private final Object dllInfo;
    // Methods
    private AltrepMethodDescriptor unserializeMethodDescriptor;
    private AltrepMethodDescriptor unserializeEXMethodDescriptor;
    private AltrepMethodDescriptor serializedStateMethodDescriptor;
    private AltrepMethodDescriptor duplicateMethodDescriptor;
    private AltrepMethodDescriptor duplicateEXMethodDescriptor;
    private AltrepMethodDescriptor coerceMethodDescriptor;
    private AltrepMethodDescriptor inspectMethodDescriptor;
    private AltrepMethodDescriptor lengthMethodDescriptor;
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

    public AltrepMethodDescriptor getDuplicateMethodDescriptor() {
        return duplicateMethodDescriptor;
    }

    public AltrepMethodDescriptor getLengthMethodDescriptor() {
        return lengthMethodDescriptor;
    }

    protected void logRegisterMethod(String methodName) {
        logger.finer(() -> "Register " + methodName + " on " + toString());
    }

    public void registerUnserializeMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Unserialize");
        this.unserializeMethodDescriptor = methodDescr;
    }

    public void registerUnserializeEXMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Unserialize_EX");
        this.unserializeEXMethodDescriptor = methodDescr;
    }

    public void registerSerializedStateMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Serialized_state");
        this.serializedStateMethodDescriptor = methodDescr;
    }

    public void registerDuplicateMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Duplicate");
        this.duplicateMethodDescriptor = methodDescr;
    }

    public void registerDuplicateEXMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Duplicate_EX");
        this.duplicateEXMethodDescriptor = methodDescr;
    }

    public void registerCoerceMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Coerce");
        this.coerceMethodDescriptor = methodDescr;
    }

    public void registerInspectMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Inspect");
        this.inspectMethodDescriptor = methodDescr;
    }

    public void registerLengthMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Length");
        this.lengthMethodDescriptor = methodDescr;
    }

    public boolean isUnserializeMethodRegistered() {
        return unserializeMethodDescriptor != null;
    }

    public boolean isUnserializeEXMethodRegistered() {
        return unserializeEXMethodDescriptor != null;
    }

    public boolean isSerializedStateMethodRegistered() {
        return serializedStateMethodDescriptor != null;
    }

    public boolean isDuplicateMethodRegistered() {
        return duplicateMethodDescriptor != null;
    }

    public boolean isDuplicateEXMethodRegistered() {
        return duplicateEXMethodDescriptor != null;
    }

    public boolean isCoerceMethodRegistered() {
        return coerceMethodDescriptor != null;
    }

    public boolean isInspectMethodRegistered() {
        return inspectMethodDescriptor != null;
    }

    public boolean isLengthMethodRegistered() {
        return lengthMethodDescriptor != null;
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

    @Override
    public String toString() {
        return packageName + ":" + className;
    }

}
