/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;

/**
 * See the documentation of {@link AltRepClassDescriptor}.
 */
public class AltRealClassDescriptor extends AltVecClassDescriptor {
    public static final String eltMethodSignature = "(pointer, sint32): double";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = false;

    public static final String getRegionMethodSignature = "(pointer, sint32, sint32, [double]): sint32";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] getRegionMethodWrapArguments = new boolean[]{true, false, false, false};
    public static final boolean getRegionMethodUnwrapResult = false;

    public static final String isSortedMethodSignature = AltIntegerClassDescriptor.isSortedMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] isSortedMethodWrapArguments = AltIntegerClassDescriptor.isSortedMethodWrapArguments;
    public static final boolean isSortedMethodUnwrapResult = AltIntegerClassDescriptor.isSortedMethodUnwrapResult;

    public static final String noNAMethodSignature = AltIntegerClassDescriptor.noNAMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] noNAMethodWrapArguments = AltIntegerClassDescriptor.noNAMethodWrapArguments;
    public static final boolean noNAMethodUnwrapResult = AltIntegerClassDescriptor.noNAMethodUnwrapResult;

    public static final String sumMethodSignature = AltIntegerClassDescriptor.sumMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] sumMethodWrapArguments = AltIntegerClassDescriptor.sumMethodWrapArguments;
    public static final boolean sumMethodUnwrapResult = AltIntegerClassDescriptor.sumMethodUnwrapResult;

    public static final String minMethodSignature = AltIntegerClassDescriptor.minMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] minMethodWrapArguments = AltIntegerClassDescriptor.minMethodWrapArguments;
    public static final boolean minMethodUnwrapResult = AltIntegerClassDescriptor.minMethodUnwrapResult;

    public static final String maxMethodSignature = AltIntegerClassDescriptor.maxMethodSignature;
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] maxMethodWrapArguments = AltIntegerClassDescriptor.maxMethodWrapArguments;
    public static final boolean maxMethodUnwrapResult = AltIntegerClassDescriptor.maxMethodUnwrapResult;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;
    private AltrepMethodDescriptor sumMethodDescriptor;
    private AltrepMethodDescriptor minMethodDescriptor;
    private AltrepMethodDescriptor maxMethodDescriptor;

    public AltRealClassDescriptor(String className, String packageName) {
        super(className, packageName);
    }

    public void registerEltMethod(AltrepMethodDescriptor eltMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.eltMethodDescriptor);
        this.eltMethodDescriptor = eltMethod;
    }

    public void registerGetRegionMethod(AltrepMethodDescriptor getRegionMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.getRegionMethodDescriptor);
        this.getRegionMethodDescriptor = getRegionMethod;
    }

    public void registerIsSortedMethod(AltrepMethodDescriptor isSortedMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.isSortedMethodDescriptor);
        this.isSortedMethodDescriptor = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepMethodDescriptor noNAMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.noNAMethodDescriptor);
        this.noNAMethodDescriptor = noNAMethod;
    }

    public void registerSumMethod(AltrepMethodDescriptor sumMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.sumMethodDescriptor);
        this.sumMethodDescriptor = sumMethod;
    }

    public void registerMaxMethod(AltrepMethodDescriptor maxMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.maxMethodDescriptor);
        this.maxMethodDescriptor = maxMethod;
    }

    public void registerMinMethod(AltrepMethodDescriptor minMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.minMethodDescriptor);
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

    public AltrepMethodDescriptor getEltMethodDescriptor() {
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

    public AltrepMethodDescriptor getMinMethodDescriptor() {
        return minMethodDescriptor;
    }

    public AltrepMethodDescriptor getMaxMethodDescriptor() {
        return maxMethodDescriptor;
    }

    @Override
    public String toString() {
        return "ALTREAL class descriptor for " + super.toString();
    }
}
