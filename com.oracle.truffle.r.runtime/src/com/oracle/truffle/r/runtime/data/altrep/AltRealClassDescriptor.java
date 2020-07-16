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

public class AltRealClassDescriptor extends AltVecClassDescriptor {
    public static final String eltMethodSignature = "(pointer, sint32): double";
    public static final String getRegionMethodSignature = "(pointer, sint32, sint32, [double]): sint32";
    public static final String isSortedMethodSignature = "(pointer): sint32";
    public static final String noNAMethodSignature = "(pointer): sint32";
    public static final String sumMethodSignature = "(pointer, sint32): pointer";
    public static final String minMethodSignature = "(pointer, sint32): pointer";
    public static final String maxMethodSignature = "(pointer, sint32): pointer";
    private Object eltMethod;
    private Object getRegionMethod;
    private Object isSortedMethod;
    private Object noNAMethod;
    private Object sumMethod;
    private Object minMethod;
    private Object maxMethod;

    public AltRealClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(Object eltMethod) {
        this.eltMethod = eltMethod;
    }

    public void registerGetRegionMethod(Object getRegionMethod) {
        this.getRegionMethod = getRegionMethod;
    }

    public void registerIsSortedMethod(Object isSortedMethod) {
        this.isSortedMethod = isSortedMethod;
    }

    public void registerNoNAMethod(Object noNAMethod) {
        this.noNAMethod = noNAMethod;
    }

    public void registerSumMethod(Object sumMethod) {
        this.sumMethod = sumMethod;
    }

    public void registerMaxMethod(Object maxMethod) {
        this.maxMethod = maxMethod;
    }

    public void registerMinMethod(Object minMethod) {
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
        return "ALTREAL class descriptor for " + super.toString();
    }
}
