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

public abstract class AltVecClassDescriptor extends AltRepClassDescriptor {
    public static final String dataptrMethodSignature = "(pointer, sint32) : pointer";
    @CompilationFinal(dimensions = 1)
    public static final boolean[] dataptrMethodWrapArguments = new boolean[]{true, false};
    public static final boolean dataptrMethodUnwrapResult = false;

    public static final String dataptrOrNullMethodSignature = "(pointer) : pointer";
    @CompilationFinal(dimensions = 1)
    public static final boolean[] dataptrOrNullMethodWrapArguments = new boolean[]{true};
    public static final boolean dataptrOrNullMethodUnwrapResult = false;

    public static final String extractSubsetMethodSignature = "(pointer, pointer, pointer) : pointer";
    @CompilationFinal(dimensions = 1)
    public static final boolean[] extractSubsetMethodWrapArguments = new boolean[]{true, true, true};
    public static final boolean extractSubsetMethodUnwrapResult = true;

    private AltrepMethodDescriptor dataptrMethodDescriptor;
    private AltrepMethodDescriptor dataptrOrNullMethodDescriptor;
    private AltrepMethodDescriptor extractSubsetMethodDescriptor;

    AltVecClassDescriptor(String className, String packageName, Object dllInfo) {
        super(className, packageName, dllInfo);
    }

    public void registerDataptrMethod(AltrepMethodDescriptor dataptrMethod) {
        logRegisterMethod("Dataptr");
        maybeInvalidateMethodRedefinedAssumption(this.dataptrMethodDescriptor);
        this.dataptrMethodDescriptor = dataptrMethod;
    }

    public void registerDataptrOrNullMethod(AltrepMethodDescriptor dataptrOrNullMethod) {
        logRegisterMethod("Dataptr_or_null");
        maybeInvalidateMethodRedefinedAssumption(this.dataptrOrNullMethodDescriptor);
        this.dataptrOrNullMethodDescriptor = dataptrOrNullMethod;
    }

    public void registerExtractSubsetMethod(AltrepMethodDescriptor extractSubsetMethod) {
        logRegisterMethod("Extract_Subset");
        maybeInvalidateMethodRedefinedAssumption(this.extractSubsetMethodDescriptor);
        this.extractSubsetMethodDescriptor = extractSubsetMethod;
    }

    public AltrepMethodDescriptor getDataptrMethodDescriptor() {
        return dataptrMethodDescriptor;
    }

    public AltrepMethodDescriptor getDataptrOrNullMethodDescriptor() {
        return dataptrOrNullMethodDescriptor;
    }

    public boolean isDataptrMethodRegistered() {
        return dataptrMethodDescriptor != null;
    }

    public boolean isDataptrOrNullMethodRegistered() {
        return dataptrOrNullMethodDescriptor != null;
    }

    public boolean isExtractSubsetMethodRegistered() {
        return extractSubsetMethodDescriptor != null;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
