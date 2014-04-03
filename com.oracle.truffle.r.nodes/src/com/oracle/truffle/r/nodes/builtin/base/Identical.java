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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Internal part of {@code identical}. The default values for args after {@code x} and {@code y} are
 * all set to {@code TRUE/FALSE} by the R snippet.
 */
@RBuiltin(".Internal.identical")
public abstract class Identical extends RBuiltinNode {

    @Specialization(order = 0)
    public Object doInternalIdentical(byte x, byte y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return RDataFactory.createLogicalVectorFromScalar(x == y);
    }

    @Specialization(order = 1)
    public Object doInternalIdential(String x, String y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return RDataFactory.createLogicalVectorFromScalar(x.equals(y));
    }

    @Specialization(order = 2)
    public Object doInternalIdentical(double x, double y,
                    // @formatter:off
                    byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        boolean truth = numEq == RRuntime.LOGICAL_TRUE ? x == y : Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y);
        return RDataFactory.createLogicalVectorFromScalar(truth);
    }

    @Specialization(order = 3)
    public Object doInternalIdentical(REnvironment x, REnvironment y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        // reference equality for environments
        return RDataFactory.createLogicalVectorFromScalar(x == y);
    }

    @Specialization(order = 100)
    public Object doInternalIdentialGeneric(@SuppressWarnings("unused") Object x, @SuppressWarnings("unused") Object y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        throw RError.getGenericError(getEncapsulatingSourceSection(), "unimplemented or invalid argument types to 'identical'");
    }

}
