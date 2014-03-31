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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin({"ls", "objects"})
public abstract class Ls extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"name", "pos", "envir", "all.names", "pattern"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(-1), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                        ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector ls(REnvironment name, Object pos, RMissing envir, byte allNames, RMissing pattern) {
        controlVisibility();
        return name.ls();
    }

    @Specialization
    @SuppressWarnings("unused")
    public RStringVector ls(VirtualFrame frame, RMissing name, int pos, RMissing envir, byte allNames, RMissing pattern) {
        controlVisibility();
        // this is the ls() specialisation
        FrameDescriptor fd = frame.getFrameDescriptor();
        String[] names = fd.getIdentifiers().toArray(RRuntime.STRING_ARRAY_SENTINEL);
        int undefinedIdentifiers = 0;
        for (int i = 0; i < names.length; ++i) {
            if (frame.getValue(fd.findFrameSlot(names[i])) == null) {
                names[i] = null;
                ++undefinedIdentifiers;
            }
        }
        String[] definedNames = new String[names.length - undefinedIdentifiers];
        int j = 0;
        for (int i = 0; i < names.length; ++i) {
            if (names[i] != null) {
                definedNames[j++] = names[i];
            }
        }
        return RDataFactory.createStringVector(definedNames, RDataFactory.COMPLETE_VECTOR);
    }

}
