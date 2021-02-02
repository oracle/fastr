/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RBaseObject;

/**
 * A base class for all the class descriptors for ALTREP. The class hierarchy of these descriptors
 * corresponds exactly to how the descriptors are managed in GNU-R.
 *
 * Every descriptor class contains public static final fields describing the ALTREP methods that
 * might be registered into that descriptor. For each ALTREP method we have to know the signature,
 * which arguments should be wrapped when we call this method, and whether to unwrap the return
 * value.
 */
public abstract class AltRepClassDescriptor extends RBaseObject {
    public static final String unserializeMethodSignature = "(pointer, pointer): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] unserializeMethodWrapArguments = new boolean[]{true, true};
    public static final boolean unserializeMethodUnwrapResult = true;

    public static final String unserializeEXMethodSignature = "(pointer, pointer, pointer, sint32, sint32): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] unserializeEXMethodWrapArguments = new boolean[]{true, true, true, false, false};
    public static final boolean unserializeEXMethodUnwrapResult = true;

    public static final String serializedStateMethodSignature = "(pointer): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] serializedStateMethodWrapArguments = new boolean[]{true};
    public static final boolean serializedStateMethodUnwrapResult = true;

    public static final String duplicateMethodSignature = "(pointer, sint32): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] duplicateMethodWrapArguments = new boolean[]{true, false};
    public static final boolean duplicateMethodUnwrapResult = true;

    public static final String duplicateEXMethodSignature = "(pointer, sint32): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] duplicateEXMethodWrapArguments = new boolean[]{true, false};
    public static final boolean duplicateEXMethodUnwrapResult = true;

    public static final String coerceMethodSignature = "(pointer, sint32): pointer";
    @CompilationFinal(dimensions = 1) public static final boolean[] coerceMethodWrapArguments = new boolean[]{true, false};
    public static final boolean coerceMethodUnwrapResult = true;

    public static final String inspectMethodSignature = "(pointer, sint32, sint32, sint32, (pointer,sint32,sint32,sint32):void): sint32";
    @CompilationFinal(dimensions = 1) public static final boolean[] inspectMethodWrapArguments = new boolean[]{true, false, false, false, false};
    public static final boolean inspectMethodUnwrapResult = false;

    public static final String lengthMethodSignature = "(pointer): sint32";
    @CompilationFinal(dimensions = 1) public static final boolean[] lengthMethodWrapArguments = new boolean[]{true};
    public static final boolean lengthMethodUnwrapResult = false;

    protected static final Assumption noMethodRedefinedAssumption = Truffle.getRuntime().createAssumption("noAltrepMethodRedefined");

    // Instance data
    private final String className;
    private final String packageName;
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

    AltRepClassDescriptor(String className, String packageName) {
        this.className = className;
        this.packageName = packageName;
    }

    @Override
    public RType getRType() {
        return RType.Raw;
    }

    public AltrepMethodDescriptor getDuplicateMethodDescriptor() {
        return duplicateMethodDescriptor;
    }

    public AltrepMethodDescriptor getLengthMethodDescriptor() {
        return lengthMethodDescriptor;
    }

    public static Assumption getNoMethodRedefinedAssumption() {
        return noMethodRedefinedAssumption;
    }

    protected void maybeInvalidateMethodRedefinedAssumption(AltrepMethodDescriptor oldDescriptor) {
        if (oldDescriptor != null) {
            noMethodRedefinedAssumption.invalidate();
        }
    }

    protected void logRegisterMethod(String methodName) {
        logger.finer(() -> "Register " + methodName + " on " + toString());
    }

    public void registerUnserializeMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Unserialize");
        maybeInvalidateMethodRedefinedAssumption(this.unserializeMethodDescriptor);
        this.unserializeMethodDescriptor = methodDescr;
    }

    public void registerUnserializeEXMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Unserialize_EX");
        maybeInvalidateMethodRedefinedAssumption(this.unserializeEXMethodDescriptor);
        this.unserializeEXMethodDescriptor = methodDescr;
    }

    public void registerSerializedStateMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Serialized_state");
        maybeInvalidateMethodRedefinedAssumption(this.serializedStateMethodDescriptor);
        this.serializedStateMethodDescriptor = methodDescr;
    }

    public void registerDuplicateMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Duplicate");
        maybeInvalidateMethodRedefinedAssumption(this.duplicateMethodDescriptor);
        this.duplicateMethodDescriptor = methodDescr;
    }

    public void registerDuplicateEXMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Duplicate_EX");
        maybeInvalidateMethodRedefinedAssumption(this.duplicateEXMethodDescriptor);
        this.duplicateEXMethodDescriptor = methodDescr;
    }

    public void registerCoerceMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Coerce");
        maybeInvalidateMethodRedefinedAssumption(this.coerceMethodDescriptor);
        this.coerceMethodDescriptor = methodDescr;
    }

    public void registerInspectMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Inspect");
        maybeInvalidateMethodRedefinedAssumption(this.inspectMethodDescriptor);
        this.inspectMethodDescriptor = methodDescr;
    }

    public void registerLengthMethod(AltrepMethodDescriptor methodDescr) {
        logRegisterMethod("Length");
        maybeInvalidateMethodRedefinedAssumption(this.lengthMethodDescriptor);
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

    @TruffleBoundary
    @Override
    public String toString() {
        return packageName + ":" + className;
    }

}
