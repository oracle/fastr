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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.ConversionFailedException;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// TODO interpret "type" and "allowNA" arguments
@RBuiltin(name = "nchar", kind = INTERNAL)
public abstract class NChar extends RBuiltinNode {

    @Child CastStringNode convertString;

    private String coerceContent(VirtualFrame frame, Object content) {
        if (convertString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            convertString = insert(CastStringNodeFactory.create(null, false, true, false, false));
        }
        try {
            return (String) convertString.executeCast(frame, content);
        } catch (ConversionFailedException e) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.CHARACTER_EXPECTED);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    public RIntVector rev(VirtualFrame frame, RNull value, String type, byte allowNA) {
        controlVisibility();
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    public int rev(VirtualFrame frame, int value, String type, byte allowNA) {
        controlVisibility();
        return coerceContent(frame, value).length();
    }

    @SuppressWarnings("unused")
    @Specialization
    public int rev(VirtualFrame frame, double value, String type, byte allowNA) {
        controlVisibility();
        return coerceContent(frame, value).length();
    }

    @SuppressWarnings("unused")
    @Specialization
    public int rev(VirtualFrame frame, byte value, String type, byte allowNA) {
        controlVisibility();
        return coerceContent(frame, value).length();
    }

    @SuppressWarnings("unused")
    @Specialization
    public RIntVector rev(RStringVector vector, String type, byte allowNA) {
        controlVisibility();
        int len = vector.getLength();
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = vector.getDataAt(i).length();
        }
        return RDataFactory.createIntVector(result, vector.isComplete(), vector.getNames());
    }

    @SuppressWarnings("unused")
    @Specialization
    public RIntVector rev(VirtualFrame frame, RAbstractVector vector, String type, byte allowNA) {
        controlVisibility();
        int len = vector.getLength();
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            result[i] = coerceContent(frame, vector.getDataAtAsObject(i)).length();
        }
        return RDataFactory.createIntVector(result, vector.isComplete(), vector.getNames());
    }
}
