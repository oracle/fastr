/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "vector", kind = INTERNAL, parameterNames = {"mode", "length"})
public abstract class Vector extends RBuiltinNode {

    private static final String CACHED_MODES_LIMIT = "3";
    private final BranchProfile errorProfile = BranchProfile.create();

    @CreateCast("arguments")
    protected RNode[] castLength(RNode[] arguments) {
        // length is at index 1
        arguments[1] = ConvertIntNodeGen.create(arguments[1]);
        return arguments;
    }

    protected RType modeToType(String mode) {
        switch (mode) {
            case "character":
                return RType.Character;
            case "logical":
                return RType.Logical;
            case "numeric":
            case "double":
                return RType.Double;
            case "integer":
                return RType.Integer;
            case "list":
                return RType.List;
            case "raw":
                return RType.Raw;
            default:
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.CANNOT_MAKE_VECTOR_OF_MODE, mode);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"mode == cachedMode"}, limit = CACHED_MODES_LIMIT)
    RAbstractVector vector(String mode, int length, @Cached("mode") String cachedMode, @Cached("modeToType(mode)") RType type) {
        controlVisibility();
        switch (type) {
            case Character:
                return RDataFactory.createStringVector(length);
            case Logical:
                return RDataFactory.createLogicalVector(length);
            case Double:
                return RDataFactory.createDoubleVector(length);
            case Integer:
                return RDataFactory.createIntVector(length);
            case List:
                Object[] data = new Object[length];
                Arrays.fill(data, RNull.instance);
                return RDataFactory.createList(data);
            case Raw:
                return RDataFactory.createRawVector(length);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    @Specialization
    protected RAbstractVector vector(String mode, int length) {
        return vector(mode, length, mode, modeToType(mode));
    }
}
