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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public abstract class AccessVarArgumentsNode extends RNode {

    protected final int index;

    protected AccessVarArgumentsNode(int index) {
        this.index = index;
    }

    public static AccessVarArgumentsNode create(LastParameterKind parameterKind, int index) {
        if (!parameterKind.isVarArgs()) {
            throw new IllegalArgumentException("No varArgs parameter kind.");
        }

        if (parameterKind == LastParameterKind.VAR_ARGS_ALWAYS_ARRAY) {
            if (index == 0) {
                return new AccessVarArgumentsArrayDirectNode();
            } else {
                return new AccessVarArgumentsArrayNode(index);
            }
        } else {
            return new AccessVarArgumentsSpecializeNode(index);
        }

    }

    private static final class AccessVarArgumentsArrayDirectNode extends AccessVarArgumentsNode {

        public AccessVarArgumentsArrayDirectNode() {
            super(0);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return executeArray(frame);
        }

        @Override
        public Object[] executeArray(VirtualFrame frame) {
            Object[] r = new Object[RArguments.getArgumentsLength(frame)];
            RArguments.copyArgumentsInto(frame, r);
            return r;
        }

    }

    private static final class AccessVarArgumentsArrayNode extends AccessVarArgumentsNode {

        public AccessVarArgumentsArrayNode(int index) {
            super(index);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return executeArray(frame);
        }

        @Override
        public Object[] executeArray(VirtualFrame frame) {
            int length = RArguments.getArgumentsLength(frame) - index;
            if (length < 1) {
                return EMPTY_OBJECT_ARRAY;
            } else {
                Object[] varArgs = new Object[length];
                for (int i = 0; i < length; i++) {
                    varArgs[i] = RArguments.getArgument(frame, i);
                }
                return varArgs;
            }
        }
    }

    private static final class AccessVarArgumentsSpecializeNode extends AccessVarArgumentsNode {

        public AccessVarArgumentsSpecializeNode(int index) {
            super(index);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            final int argLength = RArguments.getArgumentsLength(frame);
            if (index >= argLength) {
                return RMissing.instance;
            } else {
                int length = argLength - index;
                if (length == 1) {
                    return RArguments.getArgument(frame, index);
                } else {
                    Object[] varArgs = new Object[length];
                    for (int i = 0; i < length; i++) {
                        varArgs[i] = RArguments.getArgument(frame, i + index);
                    }
                    return varArgs;
                }
            }
        }
    }

}
