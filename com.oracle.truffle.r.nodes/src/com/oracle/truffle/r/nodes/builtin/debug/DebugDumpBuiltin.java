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
package com.oracle.truffle.r.nodes.builtin.debug;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.impl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Dump Truffle trees to a listening IGV instance, if any.
 */
@RBuiltin(name = "debug.dump", kind = PRIMITIVE)
@RBuiltinComment("Dumps Truffle trees to IGV if an IGV instance running.")
public abstract class DebugDumpBuiltin extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"function"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    @Override
    public final boolean getVisibility() {
        return false;
    }

    private static final int FUNCTION_LENGTH_LIMIT = 40;

    @Specialization
    public Object dump(RFunction function) {
        controlVisibility();
        String source = ((RRootNode) ((DefaultCallTarget) function.getTarget()).getRootNode()).getSourceCode();
        Utils.dumpFunction("dump: " + (source.length() <= FUNCTION_LENGTH_LIMIT ? source : source.substring(0, FUNCTION_LENGTH_LIMIT) + "..."), function);
        return RNull.instance;
    }

}
