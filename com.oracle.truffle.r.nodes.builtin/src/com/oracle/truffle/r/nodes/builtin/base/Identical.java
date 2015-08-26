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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Internal part of {@code identical}. The default values for args after {@code x} and {@code y} are
 * all default to {@code TRUE/FALSE} in the R wrapper.
 *
 * TODO Implement the full set of types. This will require refactoring the code so that a generic
 * "identical" function can be called recursively to handle lists and language objects (and
 * closures) GnuR compares attributes also. The general case is therefore slow but the fast path
 * needs to be fast! The five defaulted logical arguments are supposed to be cast to logical and
 * checked for NA (regardless of whether they are used).
 */
@RBuiltin(name = "identical", kind = INTERNAL, parameterNames = {"x", "y", "num.eq", "single.NA", "attrib.as.set", "ignore.bytecode", "ignore.environment"})
public abstract class Identical extends RBuiltinNode {

    private final ConditionProfile vecLengthProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile naArgsProfile = ConditionProfile.createBinaryProfile();

    @Specialization(guards = "isRNull(x) || isRNull(y)")
    protected byte doInternalIdentical(Object x, Object y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(byte x, byte y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(String x, String y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return x.equals(y) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(double x, double y,
                    // @formatter:off
                    byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        boolean truth = numEq == RRuntime.LOGICAL_TRUE ? x == y : Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y);
        return truth ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(REnvironment x, REnvironment y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        // reference equality for environments
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(RSymbol x, RSymbol y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return x.getName().equals(y.getName()) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(@SuppressWarnings("unused") RLanguage x, @SuppressWarnings("unused") RLanguage y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        // TODO How to compare ASTs
        throw RError.nyi(this, "language objects not supported in 'identical'");
    }

    @Specialization
    byte doInternalIdentical(RFunction x, RFunction y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        controlVisibility();
        if (naArgsProfile.profile(checkExtraArgsForNA(numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment))) {
            if (x == y) {
                return RRuntime.LOGICAL_TRUE;
            }
            if (x.isBuiltin() && !y.isBuiltin() || y.isBuiltin() && !x.isBuiltin()) {
                return RRuntime.LOGICAL_FALSE;
            }
            if (x.isBuiltin()) {
                return RRuntime.asLogical(x.getRBuiltin() == y.getRBuiltin());
            } else {
                // closures
                FunctionDefinitionNode fx = (FunctionDefinitionNode) x.getRootNode();
                FunctionDefinitionNode fy = (FunctionDefinitionNode) y.getRootNode();
                if (fx.getFormalArguments().getSignature().equals(fy.getFormalArguments().getSignature())) {
                    /*
                     * TODO check defaults and body and enclosing environment (unless
                     * ignoreEnvironment=T)
                     */
                } else {
                    return RRuntime.LOGICAL_FALSE;
                }
                throw RError.nyi(this, "identical(function)");
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "!vectorsLists(x, y)")
    protected byte doInternalIdenticalGeneric(RAbstractVector x, RAbstractVector y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        if (vecLengthProfile.profile(x.getLength() != y.getLength())) {
            return RRuntime.LOGICAL_FALSE;
        } else {
            for (int i = 0; i < x.getLength(); i++) {
                if (!x.getDataAtAsObject(i).equals(y.getDataAtAsObject(i))) {
                    return RRuntime.LOGICAL_FALSE;
                }
            }
        }
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(@SuppressWarnings("unused") RList x, @SuppressWarnings("unused") RList y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        throw RError.nyi(this, "lists not supported in 'identical'");
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(@SuppressWarnings("unused") RDataFrame x, @SuppressWarnings("unused") RDataFrame y,
                    // @formatter:off
                    @SuppressWarnings("unused") byte numEq, @SuppressWarnings("unused") byte singleNA, @SuppressWarnings("unused") byte attribAsSet,
                    @SuppressWarnings("unused") byte ignoreBytecode, @SuppressWarnings("unused") byte ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        throw RError.nyi(this, "data frames not supported in 'identical'");
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(@SuppressWarnings("unused") RFunction x, @SuppressWarnings("unused") RAbstractContainer y,
                    // @formatter:off
                    @SuppressWarnings("unused") Object numEq, @SuppressWarnings("unused") Object singleNA, @SuppressWarnings("unused") Object attribAsSet,
                    @SuppressWarnings("unused") Object ignoreBytecode, @SuppressWarnings("unused") Object ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(@SuppressWarnings("unused") RLanguage x, @SuppressWarnings("unused") RAbstractContainer y,
                    // @formatter:off
                    @SuppressWarnings("unused") Object numEq, @SuppressWarnings("unused") Object singleNA, @SuppressWarnings("unused") Object attribAsSet,
                    @SuppressWarnings("unused") Object ignoreBytecode, @SuppressWarnings("unused") Object ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(@SuppressWarnings("unused") RAbstractContainer x, @SuppressWarnings("unused") RFunction y,
                    // @formatter:off
                    @SuppressWarnings("unused") Object numEq, @SuppressWarnings("unused") Object singleNA, @SuppressWarnings("unused") Object attribAsSet,
                    @SuppressWarnings("unused") Object ignoreBytecode, @SuppressWarnings("unused") Object ignoreEnvironment) {
                    // @formatter:on
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    protected boolean vectorsLists(RAbstractVector x, RAbstractVector y) {
        return x instanceof RList && y instanceof RList;
    }

    protected boolean checkExtraArgsForNA(byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        return checkExtraArg(numEq, "num.eq") && checkExtraArg(singleNA, "single.NA") && checkExtraArg(attribAsSet, "attrib.as.set") && checkExtraArg(ignoreBytecode, "ignore.bytecode") &&
                        checkExtraArg(ignoreEnvironment, "ignore.environment");
    }

    protected boolean checkExtraArg(byte value, String name) {
        if (value == RRuntime.LOGICAL_NA) {
            throw RError.error(this, RError.Message.INVALID_VALUE, name);
        }
        return true;
    }

}
