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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.regex.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "ls", aliases = {"objects"}, kind = SUBSTITUTE, parameterNames = {"name", "pos", "envir", "all.names", "pattern"})
// TODO INTERNAL, which would sanitize the way the environment is passed to a single REnvironment
// argument
public abstract class Ls extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(-1), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                        ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RStringVector ls(VirtualFrame frame, RMissing name, int pos, RMissing envir, byte allNames, RMissing pattern) {
        controlVisibility();
        return REnvironment.createLsCurrent(frame.materialize()).ls(RRuntime.fromLogical(allNames), null);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RStringVector ls(REnvironment name, Object pos, RMissing envir, byte allNames, RMissing pattern) {
        controlVisibility();
        return name.ls(RRuntime.fromLogical(allNames), null);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RStringVector ls(VirtualFrame frame, RMissing name, int pos, REnvironment envir, byte allNames, RMissing pattern) {
        controlVisibility();
        return envir.ls(RRuntime.fromLogical(allNames), null);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RStringVector ls(VirtualFrame frame, RMissing name, int pos, RMissing envir, byte allNames, String pattern) {
        controlVisibility();
        return REnvironment.createLsCurrent(frame.materialize()).ls(RRuntime.fromLogical(allNames), compile(pattern));
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RStringVector ls(REnvironment name, Object pos, RMissing envir, byte allNames, String pattern) {
        controlVisibility();
        return name.ls(RRuntime.fromLogical(allNames), compile(pattern));
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RStringVector ls(VirtualFrame frame, RMissing name, int pos, REnvironment envir, byte allNames, String pattern) {
        controlVisibility();
        return envir.ls(RRuntime.fromLogical(allNames), compile(pattern));
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RStringVector ls(VirtualFrame frame, RAbstractIntVector name, int pos, RMissing envir, byte allNames, RMissing pattern) {
        controlVisibility();
        String[] searchPath = REnvironment.searchPath();
        REnvironment env = REnvironment.lookupOnSearchPath(searchPath[name.getDataAt(0) - 1]);
        return env.ls(RRuntime.fromLogical(allNames), null);
    }

    @SlowPath
    private static Pattern compile(String pattern) {
        return Pattern.compile(RegExp.checkPreDefinedClasses(pattern));
    }

}
