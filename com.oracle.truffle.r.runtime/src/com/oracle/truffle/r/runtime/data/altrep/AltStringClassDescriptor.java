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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public class AltStringClassDescriptor extends AltVecClassDescriptor {
    public static final String eltMethodSignature = "(pointer, sint32) : string";
    @CompilationFinal(dimensions = 1) public static final boolean[] eltMethodWrapArguments = new boolean[]{true, false};
    public static final boolean eltMethodUnwrapResult = true;

    public static final String setEltMethodSignature = "(pointer, sint32, pointer) : void";
    @CompilationFinal(dimensions = 1) public static final boolean[] setEltMethodWrapArguments = new boolean[]{true, false, true};
    public static final boolean setEltMethodUnwrapResult = false;

    public static final String isSortedMethodSignature = "(pointer) : sint32";
    @CompilationFinal(dimensions = 1) public static final boolean[] isSortedMethodWrapArguments = new boolean[]{true};
    public static final boolean isSortedMethodUnwrapResult = false;

    public static final String noNAMethodSignature = "(pointer) : sint32";
    @CompilationFinal(dimensions = 1) public static final boolean[] noNAMethodWrapArguments = new boolean[]{true};
    public static final boolean noNAMethodUnwrapResult = false;

    private AltrepMethodDescriptor eltMethodDescriptor;
    private AltrepMethodDescriptor setEltMethodDescriptor;
    private AltrepMethodDescriptor isSortedMethodDescriptor;
    private AltrepMethodDescriptor noNAMethodDescriptor;

    public AltStringClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerEltMethod(AltrepMethodDescriptor eltMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.eltMethodDescriptor);
        this.eltMethodDescriptor = eltMethod;
    }

    public void registerSetEltMethod(AltrepMethodDescriptor setEltMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.setEltMethodDescriptor);
        this.setEltMethodDescriptor = setEltMethod;
    }

    public void registerIsSortedMethod(AltrepMethodDescriptor isSortedMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.isSortedMethodDescriptor);
        this.isSortedMethodDescriptor = isSortedMethod;
    }

    public void registerNoNAMethod(AltrepMethodDescriptor noNAMethod) {
        maybeInvalidateMethodRedefinedAssumption(this.noNAMethodDescriptor);
        this.noNAMethodDescriptor = noNAMethod;
    }

    public boolean isEltMethodRegistered() {
        return eltMethodDescriptor != null;
    }

    public boolean isSetEltMethodRegistered() {
        return setEltMethodDescriptor != null;
    }

    public boolean isNoNAMethodRegistered() {
        return noNAMethodDescriptor != null;
    }

    public boolean isIsSortedMethodRegistered() {
        return isSortedMethodDescriptor != null;
    }

    public AltrepMethodDescriptor getEltMethodDescriptor() {
        return eltMethodDescriptor;
    }

    public AltrepMethodDescriptor getSetEltMethodDescriptor() {
        return setEltMethodDescriptor;
    }

    public AltrepMethodDescriptor getIsSortedMethodDescriptor() {
        return isSortedMethodDescriptor;
    }

    public AltrepMethodDescriptor getNoNAMethodDescriptor() {
        return noNAMethodDescriptor;
    }
}
