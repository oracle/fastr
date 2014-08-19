/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * A temporary substitution to work around bug with {@code list(...)} used in R version.
 */
@RBuiltin(name = "structure", kind = SUBSTITUTE, parameterNames = {".Data", "..."})
public abstract class Structure extends RBuiltinNode {
    @SuppressWarnings("unused")
    @Specialization
    protected Object structure(VirtualFrame frame, RMissing obj, RMissing args) {
        throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ARGUMENT_MISSING, ".Data");
    }

    @Specialization
    protected Object structure(VirtualFrame frame, RAbstractContainer obj, Object args) {
        if (!(args instanceof RMissing)) {
            Object[] values = args instanceof Object[] ? (Object[]) args : new Object[]{args};
            String[] argNames = getSuppliedArgsNames();
            validateArgNames(frame, argNames);
            for (int i = 0; i < values.length; i++) {
                obj.setAttr(argNames[i + 1], fixupValue(values[i]));
            }
        }
        return obj;
    }

    private static Object fixupValue(Object value) {
        if (value instanceof String) {
            return RDataFactory.createStringVectorFromScalar((String) value);
        }
        return value;
    }

    private void validateArgNames(VirtualFrame frame, String[] argNames) throws RError {
        // first "name" is the container
        boolean ok = argNames != null;
        if (argNames != null) {
            for (int i = 1; i < argNames.length; i++) {
                if (argNames[i] == null) {
                    ok = false;
                }
            }
        }
        if (!ok) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.ATTRIBUTES_NAMED);
        }
    }
}
