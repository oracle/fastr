/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 * See the documentation of {@link AltRepClassDescriptor}.
 */
public class AltRawClassDescriptor extends AltVecClassDescriptor {
    public static final String eltMethodSignature = "(pointer, sint32):uint8";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = false;

    public static final String getRegionMethodSignature = "(pointer, sint32, sint32, [uint8]):sint32";
    @CompilerDirectives.CompilationFinal(dimensions = 1) public static final boolean[] getRegionMethodWrapArguments = new boolean[]{true, false, false, false};
    public static final boolean getRegionMethodUnwrapResult = false;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor getRegionMethodDescriptor;

    public AltRawClassDescriptor(String className, String packageName) {
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

    public boolean isGetRegionMethodRegistered() {
        return getRegionMethodDescriptor != null;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getGetRegionMethodDescriptor() {
        return getRegionMethodDescriptor;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return "ALTRAW class descriptor for " + super.toString();
    }
}
