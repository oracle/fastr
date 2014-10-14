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
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * All FastR concrete types must be given a specialization here that returns {@code FALSE}, so that
 * specific subclasses need only override the types that should return {@code TRUE}. The
 * {@link #isType(Object)} method that is tagged with {@link Fallback} method effectively checks for
 * a missing specialization.
 *
 * N.B. Do not include the "abstract" types, e.g. {@link RAbstractVector} here.
 *
 * Be careful overriding this class for the case where you want to specialize an abstract type like
 * {@link RAbstractVector}. You cannot use a simple specialization with argument, say,
 * {@link RAbstractVector}, because the concrete type specializations here will be found first. Nor
 * can you override the {@link Fallback} method for the same reason. You must override all the
 * subclasses of the abstract type in the subclass. Since this is onerous, the alternative is to
 * subclass {@link RBuiltinNode} and provide a specialization for the abstract class plus a
 * {@link Fallback} that returns {@link RRuntime#LOGICAL_FALSE} for everything else.
 */
@SuppressWarnings("unused")
public abstract class IsTypeNode extends IsTypeNodeMissingAdapter {
    private static final String[] PARAMETER_NAMES = new String[]{"x"};

    @Override
    public String[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @Fallback
    protected byte isType(Object value) {
        throw RInternalError.shouldNotReachHere();
    }

    @Specialization
    protected byte isType(int value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RIntVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(double value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RDoubleVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(String value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RStringVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(byte value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RLogicalVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RNull value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RComplex value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RComplexVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RRaw value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RRawVector value) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RList value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RIntSequence value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RDoubleSequence value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RFunction value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RSymbol value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RLanguage value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RPromise value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RExpression value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RPairList value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RConnection value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(RDataFrame value) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isType(REnvironment value) {
        return RRuntime.LOGICAL_FALSE;
    }

}
