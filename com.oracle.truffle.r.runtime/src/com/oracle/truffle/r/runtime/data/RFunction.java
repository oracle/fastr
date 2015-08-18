/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;

/**
 * An instance of {@link RFunction} represents a function defined in R. The properties of a function
 * are as follows:
 * <ul>
 * <li>The {@link #name} is optional. It is only set initially for builtins (required) but (for
 * debugging) may be set later by {@link #setName}.
 * <li>The {@link #target} represents the actually callable entry point to the function.
 * <li>Functions may represent builtins; this is indicated by the {@link #builtin} flag set to the
 * associated {@link RBuiltin} instance.
 * <li>The lexically enclosing environment of this function's definition is referenced by
 * {@link #enclosingFrame}.
 * </ul>
 */
public final class RFunction extends RAttributeStorage implements RTypedValue, TruffleObject {

    private String name;
    private final RootCallTarget target;
    private final RBuiltinDescriptor builtin;
    private final boolean containsDispatch;

    private final MaterializedFrame enclosingFrame;

    RFunction(String name, RootCallTarget target, RBuiltinDescriptor builtin, MaterializedFrame enclosingFrame, boolean containsDispatch) {
        this.name = name;
        this.target = target;
        this.builtin = builtin;
        this.containsDispatch = containsDispatch;
        if (enclosingFrame instanceof VirtualEvalFrame) {
            this.enclosingFrame = ((VirtualEvalFrame) enclosingFrame).getOriginalFrame();
        } else {
            this.enclosingFrame = enclosingFrame;
        }
    }

    @Override
    public RType getRType() {
        return isBuiltin() ? RType.Builtin : RType.Closure;
    }

    public boolean isBuiltin() {
        return builtin != null;
    }

    public RBuiltinDescriptor getRBuiltin() {
        return builtin;
    }

    public boolean containsDispatch() {
        return containsDispatch;
    }

    public String getName() {
        return name;
    }

    public RootCallTarget getTarget() {
        return target;
    }

    public RootNode getRootNode() {
        return target.getRootNode();
    }

    public MaterializedFrame getEnclosingFrame() {
        return enclosingFrame;
    }

    private static final RStringVector implicitClass = RDataFactory.createStringVectorFromScalar(RType.Function.getName());

    @Override
    public RStringVector getImplicitClass() {
        return implicitClass;
    }

    @Override
    public String toString() {
        return target.toString();
    }

    public void setName(String name) {
        assert !isBuiltin();
        this.name = name;
        RContext.getRRuntimeASTAccess().setFunctionName(getRootNode(), name);
    }

    public ForeignAccess getForeignAccess() {
        return RContext.getEngine().getForeignAccess(this);
    }
}
