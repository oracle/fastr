/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin("new.env")
public abstract class NewEnv extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"hash", "parent", "size"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RMissing.instance), ConstantNode.create(29)};
    }

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // size argument is at index 2, and an int
        arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false);
        return arguments;
    }

    @Specialization
    @SuppressWarnings("unused")
    public REnvironment newEnv(byte hash, RMissing parent, int size) {
        // FIXME don't ignore hash parameter
        return new REnvironment(RRuntime.GLOBAL_ENV, size);
    }

    @Specialization
    public REnvironment newEnv(@SuppressWarnings("unused") byte hash, REnvironment parent, int size) {
        // FIXME don't ignore hash parameter
        return new REnvironment(parent, size);
    }

}
