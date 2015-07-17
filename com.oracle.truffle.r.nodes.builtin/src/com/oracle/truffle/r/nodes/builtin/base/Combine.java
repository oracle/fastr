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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.*;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.CombineNodeGen.CombineInputCastNodeGen;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "c", kind = PRIMITIVE, parameterNames = {"..."})
public abstract class Combine extends RCastingBuiltinNode {

    private static final ArgumentsSignature EMPTY_SIGNATURE = ArgumentsSignature.empty(1);

    protected static final int COMBINE_CACHED_LIMIT = PrecedenceNode.NUMBER_OF_PRECEDENCES;

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();
    @Child private CombineInputCast inputCast = CombineInputCastNodeGen.create(null);
    @Child private CastToVectorNode castVector;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object executeCombine(Object value);

    @Specialization(limit = "1", guards = {"args.getLength() == cachedLength", "cachedPrecedence == precedence(args, cachedLength)"})
    protected Object combineLengthCached(RArgsValuesAndNames args, //
                    @Cached("args.getLength()") int cachedLength, //
                    @Cached("precedence( args, cachedLength)") int cachedPrecedence, //
                    @Cached("createCast(cachedPrecedence)") CastNode cast, //
                    @Cached("createFold(cachedPrecedence)") CombineBinaryNode combineBinary) {
        controlVisibility();
        CompilerAsserts.partialEvaluationConstant(cachedPrecedence);

        if (cachedPrecedence == NO_PRECEDENCE) {
            return RNull.instance;
        }

        boolean signatureHasNames = getSuppliedSignature() != null && getSuppliedSignature().getNonNullCount() > 0;

        CompilerAsserts.partialEvaluationConstant(signatureHasNames);

        Object[] array = args.getArguments();

        Object current = readAndCast(cast, array, 0, signatureHasNames);
        for (int i = 1; i < cachedLength; i++) {
            Object other = readAndCast(cast, array, i, signatureHasNames);
            current = combineBinary.executeCombine(current, other);
        }
        RNode.reportWork(this, cachedLength);

        if (cachedPrecedence == EXPRESSION_PRECEDENCE) {
            return RDataFactory.createExpression((RList) current);
        } else {
            return current;
        }
    }

    @Specialization(contains = "combineLengthCached", limit = "COMBINE_CACHED_LIMIT", guards = {"cachedPrecedence == precedence(args, args.getLength())"})
    protected Object combineCached(RArgsValuesAndNames args, //
                    @Cached("precedence(args, args.getLength())") int cachedPrecedence, //
                    @Cached("createCast(cachedPrecedence)") CastNode cast, //
                    @Cached("createFold(cachedPrecedence)") CombineBinaryNode combineBinary) {
        return combineLengthCached(args, args.getLength(), cachedPrecedence, cast, combineBinary);
    }

    @Specialization(guards = "!isArguments(args)")
    protected Object nonArguments(Object args, @Cached("createRecursive()") Combine combine) {
        return combine.executeCombine(new RArgsValuesAndNames(new Object[]{args}, EMPTY_SIGNATURE));
    }

    private Object readAndCast(CastNode castNode, Object[] values, int index, boolean hasNames) {
        Object value = inputCast.execute(values[index]);
        if (hasNames) {
            value = namesMerge(castVector(value), getSuppliedSignature().getName(index));
        }
        return castNode.execute(value);
    }

    protected RAbstractVector namesMerge(RAbstractVector vector, String name) {
        RStringVector orgNamesObject = vector.getNames(attrProfiles);
        if (name == null) {
            return vector;
        }
        if (vector.getLength() == 0) {
            return vector;
        }
        return mergeNamesSlow(vector, name, orgNamesObject);
    }

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        RVector resultVector = ((RAbstractVector) castVector.execute(value)).materialize();
        // need to copy if vector is shared in case the same variable is used in combine, e.g. :
        // x <- 1:2 ; names(x) <- c("A",NA) ; c(x,test=x)
        if (resultVector.isShared()) {
            resultVector = resultVector.copy();
        }
        return resultVector;
    }

    protected int precedence(RArgsValuesAndNames args, int length) {
        int precedence = -1;
        Object[] array = args.getArguments();
        for (int i = 0; i < length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

    protected static boolean isArguments(Object value) {
        return value instanceof RArgsValuesAndNames;
    }

    protected Combine createRecursive() {
        return CombineNodeGen.create(null, null, null);
    }

    protected static CombineBinaryNode createFold(int precedence) {
        switch (precedence) {
            case COMPLEX_PRECEDENCE:
                return CombineBinaryComplexNodeGen.create(null, null);
            case DOUBLE_PRECEDENCE:
                return CombineBinaryDoubleNodeGen.create(null, null);
            case INT_PRECEDENCE:
                return CombineBinaryIntegerNodeGen.create(null, null);
            case LOGICAL_PRECEDENCE:
                return CombineBinaryLogicalNodeGen.create(null, null);
            case STRING_PRECEDENCE:
                return CombineBinaryStringNodeGen.create(null, null);
            case RAW_PRECEDENCE:
                return CombineBinaryRawNodeGen.create(null, null);
            case EXPRESSION_PRECEDENCE:
            case LIST_PRECEDENCE:
                return CombineBinaryListNodeGen.create(null, null);
            case NO_PRECEDENCE:
                return null;
            default:
                throw RError.nyi(null, "unsupported combine type");
        }
    }

    protected static CastNode createCast(int precedence) {
        switch (precedence) {
            case COMPLEX_PRECEDENCE:
                return CastComplexNodeGen.create(true, false, false);
            case DOUBLE_PRECEDENCE:
                return CastDoubleNodeGen.create(true, false, false);
            case INT_PRECEDENCE:
                return CastIntegerNodeGen.create(true, false, false);
            case LOGICAL_PRECEDENCE:
                return CastLogicalNodeGen.create(true, false, false);
            case STRING_PRECEDENCE:
                return CastStringNodeGen.create(false, true, false, false);
            case RAW_PRECEDENCE:
                return CastRawNodeGen.create(true, false, false);
            case EXPRESSION_PRECEDENCE:
            case LIST_PRECEDENCE:
                return CastListNodeGen.create(true, false, false);
            case NO_PRECEDENCE:
                return null;
            default:
                throw RError.nyi(null, "unsupported cast type");
        }
    }

    @TruffleBoundary
    private static RAbstractVector mergeNamesSlow(RAbstractVector vector, String name, RStringVector orgNamesObject) {
        /*
         * TODO (chumer) we should reuse some node for this to concat a RStringVector with a String.
         */
        if (orgNamesObject == null) {
            assert (name != null);
            assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
            RVector v = vector.materialize();
            if (v.getLength() == 1) {
                // single value - just use the name
                v.setNames(RDataFactory.createStringVector(new String[]{name}, true));
            } else {
                // multiple values - prepend name to the index of a given value
                String[] names = new String[v.getLength()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = name + (i + 1);
                }
                v.setNames(RDataFactory.createStringVector(names, true));
            }
            return v;
        } else {
            RStringVector orgNames = orgNamesObject;
            if (vector.getLength() == 1) {
                // single value
                RVector v = vector.materialize();
                // prepend name to the original name
                assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                String orgName = orgNames.getDataAt(0);
                v.setNames(RDataFactory.createStringVector(new String[]{orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name : name + "." + orgName}, true));
                return v;
            } else {
                // multiple values - prepend name to the index of a given value or to the original
                // name
                RVector v = vector.materialize();
                String[] names = new String[v.getLength()];
                for (int i = 0; i < names.length; i++) {
                    if (name == null) {
                        names[i] = orgNames.getDataAt(i);
                    } else {
                        assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                        String orgName = orgNames.getDataAt(i);
                        names[i] = orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name + (i + 1) : name + "." + orgName;
                    }
                }
                v.setNames(RDataFactory.createStringVector(names, true));
                return v;
            }
        }
    }

    @NodeChild
    protected abstract static class CombineInputCast extends RNode {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        public abstract Object execute(Object operand);

        @Specialization(guards = "!isVector(operand)")
        public Object pass(Object operand) {
            return operand;
        }

        protected static boolean isVector(Object operand) {
            return operand instanceof RVector;
        }

        @Specialization(guards = "needsCopy(vector)")
        public RAbstractVector noCopy(RAbstractVector vector) {
            RVector result = vector.materialize().copyDropAttributes();
            result.copyNamesFrom(attrProfiles, vector);
            return result;
        }

        @Specialization(guards = "!needsCopy(vector)")
        public RAbstractVector prepareVector(RAbstractVector vector) {
            return vector;
        }

        protected boolean needsCopy(RAbstractVector vector) {
            return vector.getAttributes() != null || vector.getNames(attrProfiles) != null || vector.getDimNames(attrProfiles) != null;
        }

    }

}
