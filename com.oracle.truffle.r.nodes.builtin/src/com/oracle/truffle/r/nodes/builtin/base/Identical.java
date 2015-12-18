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

import java.util.Iterator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

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

    protected abstract byte executeByte(Object x, Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment);

    @Child private Identical identicalRecursive;
    private final boolean recursive;

    public Identical(boolean recursive) {
        this.recursive = recursive;
    }

    private final ConditionProfile vecLengthProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile naArgsProfile = ConditionProfile.createBinaryProfile();

    private byte identicalRecursive(Object x, Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (identicalRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            identicalRecursive = insert(IdenticalNodeGen.create(true, new RNode[7], null, null));
        }
        return identicalRecursive.executeByte(x, y, numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isRNull(x) || isRNull(y)")
    protected byte doInternalIdentical(Object x, Object y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(byte x, byte y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(String x, String y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return x.equals(y) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(double x, double y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        boolean truth = numEq == RRuntime.LOGICAL_TRUE ? x == y : Double.doubleToRawLongBits(x) == Double.doubleToRawLongBits(y);
        return truth ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(RAbstractLogicalVector x, REnvironment y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(REnvironment x, RAbstractLogicalVector y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(REnvironment x, REnvironment y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        // reference equality for environments
        return x == y ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdentical(RSymbol x, RSymbol y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return x.getName().equals(y.getName()) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte doInternalIdentical(RLanguage x, RLanguage y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        if (naArgsProfile.profile(checkExtraArgsForNA(numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment))) {
            if (x == y) {
                return RRuntime.LOGICAL_TRUE;
            }
            RSyntaxNode xNode = x.getRep().asRSyntaxNode();
            RSyntaxNode yNode = y.getRep().asRSyntaxNode();
            return RRuntime.asLogical(xNode.getRequalsImpl(yNode));
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    byte doInternalIdentical(RFunction x, RFunction y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.asLogical(x == y);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!vectorsLists(x, y)")
    protected byte doInternalIdenticalGeneric(RAbstractVector x, RAbstractVector y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
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

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RList x, RList y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        if (x.getLength() != y.getLength()) {
            return RRuntime.LOGICAL_FALSE;
        }
        for (int i = 0; i < x.getLength(); i++) {
            byte res = identicalRecursive(x.getDataAt(i), y.getDataAt(i), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment);
            if (res == RRuntime.LOGICAL_FALSE) {
                return RRuntime.LOGICAL_FALSE;
            }
        }
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    protected byte doInternalIdenticalGeneric(RDataFrame x, RDataFrame y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return doInternalIdenticalGeneric(x.getVector(), y.getVector(), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RFunction x, RAbstractContainer y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RLanguage x, RAbstractContainer y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RAbstractContainer x, RFunction y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RS4Object x, RS4Object y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        if (x.isS4() != y.isS4()) {
            return RRuntime.LOGICAL_FALSE;
        }
        RAttributes xAttributes = x.getAttributes();
        RAttributes yAttributes = y.getAttributes();
        if (xAttributes.size() == yAttributes.size()) {
            Iterator<RAttribute> xIter = xAttributes.iterator();
            Iterator<RAttribute> yIter = yAttributes.iterator();
            while (xIter.hasNext()) {
                RAttribute xAttr = xIter.next();
                RAttribute yAttr = yIter.next();
                if (!xAttr.getName().equals(yAttr.getName())) {
                    return RRuntime.LOGICAL_FALSE;
                }
                byte res = identicalRecursive(xAttr.getValue(), yAttr.getValue(), numEq, singleNA, attribAsSet, ignoreBytecode, ignoreEnvironment);
                if (res == RRuntime.LOGICAL_FALSE) {
                    return RRuntime.LOGICAL_FALSE;
                }
            }
            return RRuntime.LOGICAL_TRUE;
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected byte doInternalIdenticalGeneric(RExternalPtr x, RExternalPtr y, Object numEq, Object singleNA, Object attribAsSet, Object ignoreBytecode, Object ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        return RRuntime.asLogical(x.getAddr() == y.getAddr());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected byte doInternalIdenticalWrongTypes(Object x, Object y, byte numEq, byte singleNA, byte attribAsSet, byte ignoreBytecode, byte ignoreEnvironment) {
        if (!recursive) {
            controlVisibility();
        }
        if (x.getClass() != y.getClass()) {
            return RRuntime.LOGICAL_FALSE;
        } else {
            throw RInternalError.unimplemented();
        }
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

    public static Identical create(RNode[] arguments, RBuiltinFactory builtin, ArgumentsSignature suppliedSignature) {
        return IdenticalNodeGen.create(false, arguments, builtin, suppliedSignature);
    }

}
