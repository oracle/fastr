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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("as.character")
@SuppressWarnings("unused")
public abstract class AsCharacter extends RBuiltinNode {

    private final NACheck check;

    @Child CastStringNode castCharacterNode;

    protected AsCharacter() {
        this.check = NACheck.create();
    }

    protected AsCharacter(AsCharacter other) {
        this.check = other.check;
    }

    @Specialization
    public String doInt(VirtualFrame frame, int value) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreter();
            castCharacterNode = adoptChild(CastStringNodeFactory.create(null, false));
        }
        return (String) castCharacterNode.executeString(frame, value);
    }

    @Specialization
    public String doDouble(double value) {
        check.enable(value);
        return check.convertDoubleToString(value);
    }

    @Specialization
    public String doLogical(byte value) {
        check.enable(value);
        return check.convertLogicalToString(value);
    }

    @Specialization
    public String doString(VirtualFrame frame, String value) {
        return value;
    }

    @Specialization
    public RStringVector doNull(RNull value) {
        return RDataFactory.createStringVector(0);
    }

    @Specialization
    public RStringVector doStringVector(VirtualFrame frame, RStringVector vector) {
        return vector;
    }

    @Specialization
    public RStringVector doVector(VirtualFrame frame, RAbstractVector vector) {
        if (castCharacterNode == null) {
            CompilerDirectives.transferToInterpreter();
            castCharacterNode = adoptChild(CastStringNodeFactory.create(null, false));
        }
        Object ret = castCharacterNode.executeStringVector(frame, vector);
        return (RStringVector) ret;
    }
}
