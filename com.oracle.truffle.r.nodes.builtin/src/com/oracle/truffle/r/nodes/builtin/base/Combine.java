/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.COMPLEX_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.DOUBLE_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.EXPRESSION_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.INT_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.LIST_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.LOGICAL_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.NO_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.RAW_PRECEDENCE;
import static com.oracle.truffle.r.nodes.unary.PrecedenceNode.STRING_PRECEDENCE;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.CombineNodeGen.CombineInputCastNodeGen;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.PrecedenceNode;
import com.oracle.truffle.r.nodes.unary.PrecedenceNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "c", kind = PRIMITIVE, parameterNames = {"..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class Combine extends RBuiltinNode {

    public static Combine create() {
        return CombineNodeGen.create(null);
    }

    private static final ArgumentsSignature EMPTY_SIGNATURE = ArgumentsSignature.empty(1);

    protected static final int COMBINE_CACHED_LIMIT = PrecedenceNode.NUMBER_OF_PRECEDENCES;

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();
    @Child private CombineInputCast inputCast = CombineInputCastNodeGen.create(null);
    @Child private CastToVectorNode castVector;
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object executeCombine(Object value);

    protected boolean isSimpleArguments(RArgsValuesAndNames args) {
        return !signatureHasNames(args.getSignature()) && args.getLength() == 1 && !(args.getArgument(0) instanceof RAbstractVector);
    }

    @Specialization(guards = "isSimpleArguments(args)")
    protected Object combineSimple(RArgsValuesAndNames args) {
        return args.getArgument(0);
    }

    @Specialization(contains = "combineSimple", limit = "1", guards = {"args.getSignature() == cachedSignature", "cachedPrecedence == precedence(args, cachedSignature.getLength())"})
    protected Object combineCached(RArgsValuesAndNames args, //
                    @Cached("args.getSignature()") ArgumentsSignature cachedSignature, //
                    @Cached("precedence( args, cachedSignature.getLength())") int cachedPrecedence, //
                    @Cached("createCast(cachedPrecedence)") CastNode cast, //
                    @Cached("create()") BranchProfile naBranch, //
                    @Cached("create()") BranchProfile naNameBranch, //
                    @Cached("create()") NACheck naCheck, //
                    @Cached("create()") NACheck naNameCheck, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
        CompilerAsserts.partialEvaluationConstant(cachedSignature);
        CompilerAsserts.partialEvaluationConstant(cachedPrecedence);

        if (cachedPrecedence == NO_PRECEDENCE) {
            return RNull.instance;
        }

        // perform all the casts
        Object[] elements = new Object[cachedSignature.getLength()];
        int size = prepareElements(args.getArguments(), cachedSignature, cast, cachedPrecedence, elements);

        // prepare the names (if there are any)
        RStringVector namesVector = hasNamesProfile.profile(hasNames(elements)) ? foldNames(naNameBranch, naNameCheck, elements, size) : null;

        // get the actual contents of the result
        RVector result = foldContents(cachedPrecedence, naBranch, naCheck, elements, size, namesVector);

        RNode.reportWork(this, size);

        if (cachedPrecedence == EXPRESSION_PRECEDENCE) {
            return RDataFactory.createExpression((RList) result);
        } else {
            return result;
        }
    }

    @ExplodeLoop
    private int prepareElements(Object[] args, ArgumentsSignature signature, CastNode cast, int precedence, Object[] elements) {
        boolean signatureHasNames = signatureHasNames(signature);
        CompilerAsserts.partialEvaluationConstant(signatureHasNames);

        int size = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = readAndCast(cast, args, signature, i, precedence, signatureHasNames);
            elements[i] = element;
            size += getElementSize(element);
        }
        return size;
    }

    private static int getElementSize(Object element) {
        if (element instanceof RAbstractVector) {
            return ((RAbstractVector) element).getLength();
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            return 1;
        }
    }

    @ExplodeLoop
    private boolean hasNames(Object[] elements) {
        for (Object element : elements) {
            if (element instanceof RAbstractVector) {
                RAbstractVector vector = (RAbstractVector) element;
                if (vector.getNames(attrProfiles) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @ExplodeLoop
    private RStringVector foldNames(BranchProfile naNameBranch, NACheck naNameCheck, Object[] elements, int size) {
        RStringVector result = RDataFactory.createStringVector(new String[size], true);
        result.incRefCount();
        int pos = 0;
        for (Object element : elements) {
            pos += processNamesElement(naNameBranch, naNameCheck, result, pos, element);
        }
        return result;
    }

    private int processNamesElement(BranchProfile naNameBranch, NACheck naNameCheck, RStringVector result, int pos, Object element) {
        if (element instanceof RAbstractVector) {
            RAbstractVector v = (RAbstractVector) element;
            RStringVector newNames = v.getNames(attrProfiles);
            if (newNames != null) {
                for (int i1 = 0; i1 < newNames.getLength(); i1++) {
                    result.transferElementSameType(pos + i1, newNames, i1);
                }
                if (!newNames.isComplete()) {
                    naNameBranch.enter();
                    result.setComplete(false);
                }
            } else {
                for (int i1 = 0; i1 < v.getLength(); i1++) {
                    result.updateDataAt(pos + i1, RRuntime.NAMES_ATTR_EMPTY_VALUE, naNameCheck);
                }
            }
            return v.getLength();
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            result.updateDataAt(pos, RRuntime.NAMES_ATTR_EMPTY_VALUE, naNameCheck);
            return 1;
        }
    }

    @ExplodeLoop
    private static RVector foldContents(int cachedPrecedence, BranchProfile naBranch, NACheck naCheck, Object[] elements, int size, RStringVector namesVector) {
        RVector result = createResultVector(cachedPrecedence, size, namesVector);
        int pos = 0;
        for (Object element : elements) {
            pos += processContentElement(naBranch, naCheck, result, pos, element);
        }
        return result;
    }

    private static int processContentElement(BranchProfile naBranch, NACheck naCheck, RVector result, int pos, Object element) {
        if (element instanceof RAbstractVector) {
            RAbstractVector v = (RAbstractVector) element;
            for (int i = 0; i < v.getLength(); i++) {
                result.transferElementSameType(pos + i, v, i);
            }
            if (!v.isComplete()) {
                naBranch.enter();
                result.setComplete(false);
            }
            return v.getLength();
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            naCheck.enable(true);
            result.updateDataAtAsObject(pos, element, naCheck);
            return 1;
        }
    }

    private static boolean signatureHasNames(ArgumentsSignature signature) {
        return signature != null && signature.getNonNullCount() > 0;
    }

    @TruffleBoundary
    @Specialization(limit = "COMBINE_CACHED_LIMIT", contains = "combineCached", guards = "cachedPrecedence == precedence(args)")
    protected Object combine(RArgsValuesAndNames args, //
                    @Cached("precedence(args, args.getLength())") int cachedPrecedence, //
                    @Cached("createCast(cachedPrecedence)") CastNode cast, //
                    @Cached("create()") BranchProfile naBranch, //
                    @Cached("create()") BranchProfile naNameBranch, //
                    @Cached("create()") NACheck naCheck, //
                    @Cached("create()") NACheck naNameCheck, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
        return combineCached(args, args.getSignature(), cachedPrecedence, cast, naBranch, naNameBranch, naCheck, naNameCheck, hasNamesProfile);
    }

    @Specialization(guards = "!isArguments(args)")
    protected Object nonArguments(Object args, @Cached("createRecursive()") Combine combine) {
        return combine.executeCombine(new RArgsValuesAndNames(new Object[]{args}, EMPTY_SIGNATURE));
    }

    private Object readAndCast(CastNode castNode, Object[] values, ArgumentsSignature signature, int index, int precedence, boolean hasNames) {
        Object value = inputCast.execute(values[index]);
        if (hasNames) {
            value = namesMerge(castVector(value), signature.getName(index));
        }
        return (precedence == EXPRESSION_PRECEDENCE && value instanceof RLanguage) ? value : castNode.execute(value);
    }

    private RAbstractVector namesMerge(RAbstractVector vector, String name) {
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

    protected int precedence(RArgsValuesAndNames args) {
        int precedence = NO_PRECEDENCE;
        Object[] array = args.getArguments();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], false));
        }
        return precedence;
    }

    @ExplodeLoop
    protected int precedence(RArgsValuesAndNames args, int length) {
        int precedence = NO_PRECEDENCE;
        Object[] array = args.getArguments();
        for (int i = 0; i < length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], false));
        }
        return precedence;
    }

    protected static boolean isArguments(Object value) {
        return value instanceof RArgsValuesAndNames;
    }

    protected Combine createRecursive() {
        return CombineNodeGen.create(null);
    }

    private static RVector createResultVector(int precedence, int size, RStringVector names) {
        switch (precedence) {
            case COMPLEX_PRECEDENCE:
                return RDataFactory.createComplexVector(new double[size * 2], true, names);
            case DOUBLE_PRECEDENCE:
                return RDataFactory.createDoubleVector(new double[size], true, names);
            case INT_PRECEDENCE:
                return RDataFactory.createIntVector(new int[size], true, names);
            case LOGICAL_PRECEDENCE:
                return RDataFactory.createLogicalVector(new byte[size], true, names);
            case STRING_PRECEDENCE:
                return RDataFactory.createStringVector(new String[size], true, names);
            case RAW_PRECEDENCE:
                return RDataFactory.createRawVector(new byte[size], names);
            case EXPRESSION_PRECEDENCE:
            case LIST_PRECEDENCE:
                return RDataFactory.createList(new Object[size], names);
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
    private static RAbstractVector mergeNamesSlow(RAbstractVector vector, String name, RStringVector orgNames) {
        /*
         * TODO (chumer) we should reuse some node for this to concat a RStringVector with a String.
         */
        RVector v = vector.materialize();
        RStringVector newNames;
        if (orgNames == null) {
            assert (name != null);
            assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
            if (v.getLength() == 1) {
                // single value - just use the name
                newNames = RDataFactory.createStringVector(new String[]{name}, true);
            } else {
                // multiple values - prepend name to the index of a given value
                String[] names = new String[v.getLength()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = name + (i + 1);
                }
                newNames = RDataFactory.createStringVector(names, true);
            }
        } else {
            if (vector.getLength() == 1) {
                // single value
                // prepend name to the original name
                assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                String orgName = orgNames.getDataAt(0);
                newNames = RDataFactory.createStringVector(new String[]{orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name : name + "." + orgName}, true);
            } else {
                // multiple values - prepend name to the index of a given value or to the original
                // name
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
                newNames = RDataFactory.createStringVector(names, true);
            }
        }
        v.setNames(newNames);
        return v;
    }

    @NodeChild
    protected abstract static class CombineInputCast extends RNode {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        public abstract Object execute(Object operand);

        @Specialization(guards = "!isVector(operand)")
        protected Object pass(Object operand) {
            return operand;
        }

        protected static boolean isVector(Object operand) {
            return operand instanceof RVector;
        }

        @Specialization(guards = "needsCopy(vector)")
        protected RAbstractVector noCopy(RAbstractVector vector, //
                        @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile, //
                        @Cached("createBinaryProfile()") ConditionProfile hasDimNamesProfile) {
            RVector materialized = vector.materialize();
            RVector result = materialized.copyDropAttributes();

            RStringVector vecNames = materialized.getInternalNames();
            if (hasNamesProfile.profile(vecNames != null)) {
                result.initAttributes(RAttributes.createInitialized(new String[]{RRuntime.NAMES_ATTR_KEY}, new Object[]{vecNames}));
                result.setInternalNames(vecNames);
            } else {
                RList dimNames = materialized.getInternalDimNames();
                if (hasDimNamesProfile.profile(dimNames != null)) {
                    result.initAttributes(RAttributes.createInitialized(new String[]{RRuntime.DIMNAMES_ATTR_KEY}, new Object[]{dimNames}));
                    result.setInternalDimNames(dimNames);
                }
            }
            return result;
        }

        @Specialization(guards = "!needsCopy(vector)")
        protected RAbstractVector prepareVector(RAbstractVector vector) {
            return vector;
        }

        protected boolean needsCopy(RAbstractVector vector) {
            return vector.getAttributes() != null || vector.getNames(attrProfiles) != null || vector.getDimNames(attrProfiles) != null;
        }
    }
}
