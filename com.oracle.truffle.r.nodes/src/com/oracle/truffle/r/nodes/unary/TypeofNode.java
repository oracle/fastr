/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@SuppressWarnings("unused")
public abstract class TypeofNode extends UnaryNode {

    public abstract RType execute(VirtualFrame frame, Object x);

    @Specialization
    protected RType typeof(RNull vector) {
        return RType.Null;
    }

    @Specialization
    protected RType typeof(byte x) {
        return RType.Logical;
    }

    @Specialization
    protected RType typeof(int s) {
        return RType.Integer;
    }

    @Specialization
    protected RType typeof(double x) {
        return RType.Double;
    }

    @Specialization
    protected RType typeof(RComplex x) {
        return RType.Complex;
    }

    @Specialization
    protected RType typeof(RRaw x) {
        return RType.Raw;
    }

    @Specialization
    protected RType typeof(String x) {
        return RType.Character;
    }

    @Specialization
    protected RType typeof(RIntSequence vector) {
        return RType.Integer;
    }

    @Specialization
    protected RType typeof(RLogicalVector vector) {
        return RType.Logical;
    }

    @Specialization
    protected RType typeof(RAbstractIntVector vector) {
        return RType.Integer;
    }

    @Specialization
    protected RType typeof(RAbstractDoubleVector vector) {
        return RType.Double;
    }

    @Specialization
    protected RType typeof(RStringVector vector) {
        return RType.Character;
    }

    @Specialization
    protected RType typeof(RComplexVector vector) {
        return RType.Complex;
    }

    @Specialization
    protected RType typeof(RRawVector vector) {
        return RType.Raw;
    }

    @Specialization
    protected RType typeof(RList list) {
        return RType.List;
    }

    @Specialization
    protected RType typeof(REnvironment env) {
        return RType.Environment;
    }

    @Specialization
    protected RType typeof(RSymbol symbol) {
        return RType.Symbol;
    }

    @Specialization
    protected RType typeof(RExternalPtr symbol) {
        return RType.ExternalPtr;
    }

    @Specialization
    protected RType typeof(RLanguage language) {
        return RType.Language;
    }

    @Specialization
    protected RType typeof(RPromise promise) {
        return RType.Promise;
    }

    @Specialization
    protected RType typeof(RExpression symbol) {
        return RType.Expression;
    }

    @Specialization
    protected RType typeof(RPairList pairlist) {
        return RType.PairList;
    }

    @Specialization(guards = "isFunctionBuiltin")
    protected RType typeofBuiltin(RFunction obj) {
        return RType.Builtin;
    }

    @Specialization(guards = "!isFunctionBuiltin")
    protected RType typeofClosure(RFunction obj) {
        return RType.Closure;
    }

    @Specialization
    protected RType typeofFormula(RFormula f) {
        return RType.Language;
    }

    @Specialization
    protected RType typeof(RConnection conn) {
        return RType.Integer;
    }

    @Specialization
    protected RType typeof(RDataFrame frame) {
        return RType.List;
    }

    @Specialization
    protected RType typeof(RFactor factor) {
        return RType.Integer;
    }

    public static boolean isFunctionBuiltin(RFunction fun) {
        return fun.isBuiltin();
    }
}
