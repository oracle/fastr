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
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;

import java.util.Arrays;
import java.util.logging.Level;

public abstract class AltRepClassDescriptor extends RBaseObject {
    private final String lengthMethodSignature = "(pointer):sint32";
    private final int lengthMethodArgCount = 1;
    private final String duplicateMethodSignature = "(pointer, sint32): pointer";
    private final int duplicateMethodArgCount = 2;
    private String className;
    private String packageName;
    private Object dllInfo;
    private Object unserializeMethod;
    private Object unserializeEXMethod;
    private Object serializedStateMethod;
    private Object duplicateMethod;
    private Object duplicateEXMethod;
    private Object coerceMethod;
    private Object inspectMethod;
    private Object lengthMethod;
    // TODO: Interop as a private field. (One interop for every method?)
    private static final TruffleLogger logger = RLogger.getLogger("altrep");

    // TODO: Pridat nejakej check ze nekdo zaregistroval vsechny metody, ktery mel.
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

    public Object getDuplicateMethod() {
        return duplicateMethod;
    }

    public Object getLengthMethod() {
        return lengthMethod;
    }

    protected void logRegisterMethod(String methodName) {
        logger.finer(() -> "Register " + methodName + " on " + toString());
    }

    public void registerUnserializeMethod(Object method) {
        logRegisterMethod("Unserialize");
        this.unserializeMethod = method;
    }

    public void registerUnserializeEXMethod(Object method) {
        logRegisterMethod("Unserialize_EX");
        this.unserializeEXMethod = method;
    }

    public void registerSerializedStateMethod(Object method) {
        logRegisterMethod("Serialized_state");
        this.serializedStateMethod = method;
    }

    public void registerDuplicateMethod(Object method) {
        logRegisterMethod("Duplicate");
        this.duplicateMethod = method;
    }

    public void registerDuplicateEXMethod(Object method) {
        logRegisterMethod("Duplicate_EX");
        this.duplicateEXMethod = method;
    }

    public void registerCoerceMethod(Object method) {
        logRegisterMethod("Coerce");
        this.coerceMethod = method;
    }

    public void registerInspectMethod(Object method) {
        logRegisterMethod("Inspect");
        this.inspectMethod = method;
    }

    public void registerLengthMethod(Object lengthMethod) {
        logRegisterMethod("Length");
        this.lengthMethod = lengthMethod;
    }

    public boolean isUnserializeMethodRegistered() {
        return unserializeMethod != null;
    }

    public boolean isUnserializeEXMethodRegistered() {
        return unserializeEXMethod != null;
    }

    public boolean isSerializedStateMethodRegistered() {
        return serializedStateMethod != null;
    }

    public boolean isDuplicateMethodRegistered() {
        return duplicateMethod != null;
    }

    public boolean isDuplicateEXMethodRegistered() {
        return duplicateEXMethod != null;
    }

    public boolean isCoerceMethodRegistered() {
        return coerceMethod != null;
    }

    public boolean isInspectMethodRegistered() {
        return inspectMethod != null;
    }

    public boolean isLengthMethodRegistered() {
        return lengthMethod != null;
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
    static Object invokeNativeFunction(InteropLibrary interop, Object function, String functionSignature, int argLen, Object instance, Object... args) {
        assert instance instanceof RBaseObject;
        NativeDataAccess.NativeMirror mirror = wrapInNativeMirror((RBaseObject) instance);
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
        } catch (Exception e) {
            throw RInternalError.shouldNotReachHere("Exception in invoke...Method");
        }
    }

    public Object invokeDuplicateMethod(Object instance, InteropLibrary interop, boolean deep) {
        interop = interop == null ? InteropLibrary.getFactory().getUncached(duplicateMethod) : interop;
        Object ret = invokeNativeFunction(interop, duplicateMethod, duplicateMethodSignature, duplicateMethodArgCount, instance, deep);
        // TODO: Return type checks?
        return ret;
    }

    public int invokeLengthMethod(Object instance, InteropLibrary lengthMethodInterop) {
        lengthMethodInterop = lengthMethodInterop == null ? InteropLibrary.getFactory().getUncached(lengthMethod) : lengthMethodInterop;
        if (logger.isLoggable(Level.FINER)) {
            logBeforeInteropExecute("lengthMethod", instance);
        }
        Object ret = invokeNativeFunction(lengthMethodInterop, lengthMethod, lengthMethodSignature, lengthMethodArgCount, instance);
        if (logger.isLoggable(Level.FINER)) {
            logAfterInteropExecute(ret);
        }
        assert ret instanceof Long;
        return ((Long) ret).intValue();
    }

    private static NativeDataAccess.NativeMirror wrapInNativeMirror(RBaseObject altrepInstance) {
        NativeDataAccess.NativeMirror mirror = altrepInstance.getNativeMirror();
        // TODO: Binary condition profile
        if (mirror == null) {
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
