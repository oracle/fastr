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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings("unused")
@RBuiltin(name = "gettext", kind = INTERNAL, parameterNames = {"...", "domain"})
public abstract class GetText extends RBuiltinNode {

    @Child private CastToVectorNode castVector;
    @Child private CastStringNode castString;

    private final NACheck elementNACheck = NACheck.create();

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, false, true, false, false));
        }
        return castString.executeCast(frame, operand);
    }

    private Object castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
        }
        return castVector.executeObject(frame, value);
    }

    @Specialization
    protected RStringVector getText(VirtualFrame frame, Object args, Object domain) {
        // no translation done at this point
        // TODO: cannot specify args as RArgsValuesAndNames due to annotation processor error
        RArgsValuesAndNames varargs = (RArgsValuesAndNames) args;
        Object[] argValues = varargs.getValues();
        String[] a = new String[0];
        int aLength = 0;
        int index = 0;
        for (int i = 0; i < argValues.length; i++) {
            Object v = castVector(frame, argValues[i]);
            if (v != RNull.instance) {
                RStringVector vector = (RStringVector) castString(frame, v);
                elementNACheck.enable(vector);
                aLength += vector.getLength();
                a = Utils.resizeArray(a, Math.max(aLength, a.length * 2));
                for (int j = 0; j < vector.getLength(); j++) {
                    a[index] = vector.getDataAt(j);
                    elementNACheck.check(a[index]);
                    index++;
                }
            }
        }

        return RDataFactory.createStringVector(a.length == aLength ? a : Arrays.copyOf(a, aLength), elementNACheck.neverSeenNA());
    }
}
