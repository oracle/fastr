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

import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.ops.*;

public class BasePackage extends RBuiltinPackage {

    public BasePackage(REnvironment env) {
        super(env);
        // primitive operations
        load(UnaryNotNode.class).names("!");
        load(BinaryArithmeticNode.class).names("+").arguments(BinaryArithmetic.ADD, null);
        load(BinaryArithmeticNode.class).names("-").arguments(BinaryArithmetic.SUBTRACT, UnaryArithmetic.NEGATE);
        load(BinaryArithmeticNode.class).names("/").arguments(BinaryArithmetic.DIV, null);
        load(BinaryArithmeticNode.class).names("%/%").arguments(BinaryArithmetic.INTEGER_DIV, null);
        load(BinaryArithmeticNode.class).names("%%").arguments(BinaryArithmetic.MOD, null);
        load(BinaryArithmeticNode.class).names("*").arguments(BinaryArithmetic.MULTIPLY, null);
        load(BinaryArithmeticNode.class).names("^").arguments(BinaryArithmetic.POW, null);
        load(BinaryBooleanNode.class).names("==").arguments(BinaryCompare.EQUAL);
        load(BinaryBooleanNode.class).names("!=").arguments(BinaryCompare.NOT_EQUAL);
        load(BinaryBooleanNode.class).names(">=").arguments(BinaryCompare.GREATER_EQUAL);
        load(BinaryBooleanNode.class).names(">").arguments(BinaryCompare.GREATER_THAN);
        load(BinaryBooleanNode.class).names("<").arguments(BinaryCompare.LESS_THAN);
        load(BinaryBooleanNode.class).names("<=").arguments(BinaryCompare.LESS_EQUAL);

        load(BinaryBooleanNonVectorizedNode.class).names("&&").arguments(BinaryLogic.NON_VECTOR_AND);
        load(BinaryBooleanNonVectorizedNode.class).names("||").arguments(BinaryLogic.NON_VECTOR_OR);
        load(BinaryBooleanNode.class).names("&").arguments(BinaryLogic.AND);
        load(BinaryBooleanNode.class).names("|").arguments(BinaryLogic.OR);

        loadBuiltins();

    }

    @Override
    public String getName() {
        return "base";
    }

}
