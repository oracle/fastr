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

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;
    private AltrepMethodDescriptor sumMethodDescriptor;
    private AltrepMethodDescriptor minMethodDescriptor;
    private AltrepMethodDescriptor maxMethodDescriptor;

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

}
