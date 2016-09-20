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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
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

    private static final int MAX_PROFILES = 4;

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();
    @Child private CombineInputCast inputCast = CombineInputCastNodeGen.create(null);
    @Child private CastToVectorNode castVector;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile naBranch = BranchProfile.create();
    private final NACheck naCheck = NACheck.create();
    private final ConditionProfile fastNamesMerge = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isAbstractVectorProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasNewNamesProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private final ValueProfile[] argProfiles = new ValueProfile[MAX_PROFILES];

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
                    @Cached("create()") BranchProfile naNameBranch, //
                    @Cached("create()") NACheck naNameCheck, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
        CompilerAsserts.partialEvaluationConstant(cachedSignature);
        CompilerAsserts.partialEvaluationConstant(cachedPrecedence);

        if (cachedPrecedence == NO_PRECEDENCE) {
            return RNull.instance;
        }

        // perform all the casts
        Object[] elements = new Object[cachedSignature.getLength()];
        int size = prepareElements(args.getArguments(), cast, cachedPrecedence, elements);

        // prepare the names (if there are any)
        boolean signatureHasNames = signatureHasNames(cachedSignature);
        CompilerAsserts.partialEvaluationConstant(signatureHasNames);
        RStringVector namesVector = hasNamesProfile.profile(signatureHasNames || hasNames(elements)) ? foldNames(naNameBranch, naNameCheck, elements, size, cachedSignature) : null;

        // get the actual contents of the result
        RVector result = foldContents(cachedPrecedence, elements, size, namesVector);

        RNode.reportWork(this, size);

        return result;
    }

    @ExplodeLoop
    private int prepareElements(Object[] args, CastNode cast, int precedence, Object[] elements) {
        int size = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = readAndCast(cast, args[i], precedence);
            if (i < argProfiles.length) {
                if (argProfiles[i] == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    argProfiles[i] = ValueProfile.createClassProfile();
                }
                element = argProfiles[i].profile(element);
            }
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
        for (int i = 0; i < elements.length; i++) {
            Object element = elements[i];
            if (i < argProfiles.length) {
                element = argProfiles[i].profile(element);
            }
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
    private RStringVector foldNames(BranchProfile naNameBranch, NACheck naNameCheck, Object[] elements, int size, ArgumentsSignature signature) {
        RStringVector result = RDataFactory.createStringVector(new String[size], true);
        result.incRefCount();
        int pos = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = elements[i];
            if (i < argProfiles.length) {
                element = argProfiles[i].profile(element);
            }
            pos += processNamesElement(naNameBranch, naNameCheck, result, pos, element, i, signature);
        }
        return result;
    }

    private int processNamesElement(BranchProfile naNameBranch, NACheck naNameCheck, RStringVector result, int pos, Object element, int index, ArgumentsSignature signature) {
        String signatureName = signature.getName(index);
        if (element instanceof RAbstractVector) {
            RAbstractVector v = (RAbstractVector) element;
            int length = v.getLength();

            RStringVector newNames = v.getNames(attrProfiles);
            if (signatureName != null && length > 0) {
                if (fastNamesMerge.profile(length == 1 && newNames == null)) {
                    newNames = RDataFactory.createStringVector(new String[]{signatureName}, true);
                } else {
                    newNames = RDataFactory.createStringVector(mergeNamesSlow(length, signatureName, newNames), true);
                }
            }
            if (hasNewNamesProfile.profile(newNames != null)) {
                for (int i1 = 0; i1 < length; i1++) {
                    result.transferElementSameType(pos + i1, newNames, i1);
                }
                if (!newNames.isComplete()) {
                    naNameBranch.enter();
                    result.setComplete(false);
                }
            } else {
                for (int i1 = 0; i1 < length; i1++) {
                    result.updateDataAt(pos + i1, RRuntime.NAMES_ATTR_EMPTY_VALUE, naNameCheck);
                }
            }
            return v.getLength();
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            String name = signatureName != null ? signatureName : RRuntime.NAMES_ATTR_EMPTY_VALUE;
            result.updateDataAt(pos, name, naNameCheck);
            return 1;
        }
    }

    @ExplodeLoop
    private RVector foldContents(int cachedPrecedence, Object[] elements, int size, RStringVector namesVector) {
        RVector result = createResultVector(cachedPrecedence, size, namesVector);
        int pos = 0;
        for (Object element : elements) {
            pos += processContentElement(result, pos, element);
        }
        return result;
    }

    private int processContentElement(RVector result, int pos, Object element) {
        if (isAbstractVectorProfile.profile(element instanceof RAbstractVector)) {
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
                    @Cached("create()") BranchProfile naNameBranch, //
                    @Cached("create()") NACheck naNameCheck, //
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile) {
        return combineCached(args, args.getSignature(), cachedPrecedence, cast, naNameBranch, naNameCheck, hasNamesProfile);
    }

    @Specialization(guards = "!isArguments(args)")
    protected Object nonArguments(Object args, @Cached("createRecursive()") Combine combine) {
        return combine.executeCombine(new RArgsValuesAndNames(new Object[]{args}, EMPTY_SIGNATURE));
    }

    private Object readAndCast(CastNode castNode, Object arg, int precedence) {
        Object value = inputCast.execute(arg);
        return (precedence == EXPRESSION_PRECEDENCE && value instanceof RLanguage) ? value : castNode.execute(value);
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
                return RDataFactory.createExpression(new Object[size], names);
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
                return CastStringNodeGen.create(true, false, false);
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
    private static String[] mergeNamesSlow(int length, String name, RStringVector orgNames) {
        String[] names = new String[length];
        if (orgNames == null) {
            assert (name != null);
            assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
            if (length == 1) {
                // single value - just use the name
                names[0] = name;
            } else {
                // multiple values - prepend name to the index of a given value
                for (int i = 0; i < names.length; i++) {
                    names[i] = name + (i + 1);
                }
                return names;
            }
        } else {
            if (length == 1) {
                // single value - prepend name to the original name
                assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                String orgName = orgNames.getDataAt(0);
                names[0] = orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name : name + "." + orgName;
            } else {
                // multiple values - prepend name to the index of a given value or to the original
                // name
                for (int i = 0; i < names.length; i++) {
                    if (name == null) {
                        names[i] = orgNames.getDataAt(i);
                    } else {
                        assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                        String orgName = orgNames.getDataAt(i);
                        names[i] = orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name + (i + 1) : name + "." + orgName;
                    }
                }
            }
        }
        return names;
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
