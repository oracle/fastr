/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractDimNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastListNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastRawNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.PrecedenceNode;
import com.oracle.truffle.r.nodes.unary.PrecedenceNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessWriteIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class, DSLConfig.class})
@RBuiltin(name = "c", kind = PRIMITIVE, parameterNames = {"...", "recursive"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class Combine extends RBuiltinNode.Arg2 {

    public static Combine create() {
        return CombineNodeGen.create();
    }

    private static final ArgumentsSignature EMPTY_SIGNATURE = ArgumentsSignature.empty(1);

    protected static final int COMBINE_CACHED_SIGNATURE_LIMIT = 1;
    protected static final int COMBINE_CACHED_PRECEDENCE_LIMIT = PrecedenceNode.NUMBER_OF_PRECEDENCES;

    private static final int MAX_PROFILES = 8;

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();

    @Children private final CombineInputCast[] inputCasts = new CombineInputCast[MAX_PROFILES];
    @Child private CombineInputCast overflowInputCast;

    @Children private final VectorDataLibrary[] elementsDataLibs = new VectorDataLibrary[MAX_PROFILES];
    @Child private VectorDataLibrary overflowElementDataLib;

    @Child private VectorDataLibrary vectorDataLibrary;

    private final ConditionProfile fastNamesMerge = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isAbstractVectorProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile hasNewNamesProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(Combine.class);
        casts.arg("...").mustBeValidVarArgs();
        casts.arg("recursive").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
    }

    public abstract Object executeCombine(Object value, Object recursive);

    protected boolean isSimpleArguments(RArgsValuesAndNames args) {
        return !signatureHasNames(args.getSignature()) &&
                        args.getLength() == 1 &&
                        !(args.getArgument(0) instanceof RAbstractVector) &&
                        !(args.getArgument(0) instanceof REnvironment) &&
                        !(args.getArgument(0) instanceof RFunction) &&
                        !(args.getArgument(0) instanceof RPairList) &&
                        !(args.getArgument(0) instanceof RSymbol) &&
                        !(args.getArgument(0) instanceof RS4Object) &&
                        !(args.getArgument(0) instanceof CharSXPWrapper) &&
                        !RRuntime.isForeignObject(args.getArgument(0));
    }

    @Specialization(guards = {"isSimpleArguments(args)", "!recursive"})
    protected Object combineSimple(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean recursive) {
        return args.getArgument(0);
    }

    private CombineInputCast getCast(int index) {
        CombineInputCast cast = index < MAX_PROFILES ? inputCasts[index] : overflowInputCast;
        if (cast == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cast = insert(new CombineInputCast());
            if (index < MAX_PROFILES) {
                inputCasts[index] = cast;
            } else {
                overflowInputCast = cast;
            }
        }
        return cast;
    }

    private VectorDataLibrary getElemDataLib(int index) {
        VectorDataLibrary lib = index < MAX_PROFILES ? elementsDataLibs[index] : overflowElementDataLib;
        if (lib == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lib = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
            if (index < MAX_PROFILES) {
                elementsDataLibs[index] = lib;
            } else {
                overflowElementDataLib = lib;
            }
        }
        return lib;
    }

    @Specialization(replaces = "combineSimple", limit = "getCacheSize(COMBINE_CACHED_SIGNATURE_LIMIT)", guards = {"!recursive", "args.getSignature() == cachedSignature",
                    "cachedPrecedence == precedence(args, cachedSignature.getLength())"})
    protected Object combineCached(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean recursive,
                    @Cached("args.getSignature()") ArgumentsSignature cachedSignature,
                    @Cached("precedence(args, cachedSignature.getLength())") int cachedPrecedence,
                    @Cached("createCast(cachedPrecedence)") CastNode cast,
                    @Cached("create()") BranchProfile naNameBranch,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
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
        RStringVector namesVector = hasNamesProfile.profile(signatureHasNames || hasNames(elements, getNamesNode)) ? foldNames(naNameBranch, elements, size, cachedSignature, getNamesNode)
                        : null;

        // get the actual contents of the result
        RAbstractVector result = foldContents(cachedPrecedence, elements, size, namesVector);

        RBaseNode.reportWork(this, size);

        return result;
    }

    @TruffleBoundary
    @Specialization(limit = "getCacheSize(COMBINE_CACHED_PRECEDENCE_LIMIT)", replaces = "combineCached", guards = {"!recursive", "cachedPrecedence == precedence(args)"})
    protected Object combine(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean recursive,
                    @Cached("precedence(args, args.getLength())") int cachedPrecedence,
                    @Cached("createCast(cachedPrecedence)") CastNode cast,
                    @Cached("create()") BranchProfile naNameBranch,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile,
                    @Cached("create()") GetNamesAttributeNode getNamesNode) {
        return combineCached(args, false, args.getSignature(), cachedPrecedence, cast, naNameBranch, hasNamesProfile, getNamesNode);
    }

    @TruffleBoundary
    @Specialization(replaces = "combine", guards = {"!recursive"})
    protected Object combineGeneric(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean recursive,
                    @Cached("create()") BranchProfile naNameBranch,
                    @Cached("createBinaryProfile()") ConditionProfile hasNamesProfile,
                    @Cached("create()") GetNamesAttributeNode getNamesNode,
                    @Cached("create()") GenericCastNode genericCastNode) {
        int cachedPrecedence = precedence(args, args.getLength());
        return combineCached(args, false, args.getSignature(), cachedPrecedence, genericCastNode.get(cachedPrecedence), naNameBranch, hasNamesProfile, getNamesNode);
    }

    @Specialization(guards = "recursive")
    protected Object combineRecursive(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean recursive,
                    @Cached("create()") Combine recursiveCombine,
                    @Cached("createBinaryProfile()") ConditionProfile useNewArgsProfile) {
        return combineRecursive(args, recursiveCombine, useNewArgsProfile);
    }

    private static Object combineRecursive(RArgsValuesAndNames args, Combine recursiveCombine, ConditionProfile useNewArgsProfile) {
        Object[] argsArray = args.getArguments();
        Object[] newArgsArray = new Object[argsArray.length];
        boolean useNewArgs = false;
        for (int i = 0; i < argsArray.length; i++) {
            Object arg = argsArray[i];
            if (arg instanceof RList) {
                Object[] argsFromList = ((RList) arg).getReadonlyData();
                newArgsArray[i] = recursiveCombine.executeCombine(new RArgsValuesAndNames(argsFromList,
                                ArgumentsSignature.empty(argsFromList.length)), true);
                useNewArgs = true;
            } else {
                newArgsArray[i] = arg;
            }
        }

        if (useNewArgsProfile.profile(useNewArgs)) {
            return recursiveCombine.executeCombine(new RArgsValuesAndNames(newArgsArray,
                            args.getSignature()), false);
        } else {
            return recursiveCombine.executeCombine(args, false);
        }
    }

    @ExplodeLoop
    private int prepareElements(Object[] args, CastNode cast, int precedence, Object[] elements) {
        int size = 0;
        boolean exprListPrecedence = precedence == EXPRESSION_PRECEDENCE || precedence == LIST_PRECEDENCE;
        for (int i = 0; i < elements.length; i++) {
            CombineInputCast inputCast = getCast(i);
            Object value = args[i];
            Object element = (exprListPrecedence && (value instanceof RPairList && ((RPairList) value).isLanguage())) ? value : cast.doCast(inputCast.cast(value, i));
            element = inputCast.valueProfile.profile(element);
            elements[i] = element;
            size += getElementSize(element, i);
        }
        return size;
    }

    private int getElementSize(Object element, int elementIndex) {
        if (element instanceof RAbstractVector) {
            return getElemDataLib(elementIndex).getLength(((RAbstractVector) element).getData());
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            return 1;
        }
    }

    @ExplodeLoop
    private boolean hasNames(Object[] elements, GetNamesAttributeNode getNamesNode) {
        for (int i = 0; i < elements.length; i++) {
            Object element = getCast(i).valueProfile.profile(elements[i]);
            if (element instanceof RAbstractVector) {
                RAbstractVector vector = (RAbstractVector) element;
                if (getNamesNode.getNames(vector) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class FoldedNames {
        final String[] names;
        boolean complete = true;

        FoldedNames(String[] array) {
            this.names = array;
        }
    }

    @ExplodeLoop
    private RStringVector foldNames(BranchProfile naNameBranch, Object[] elements, int size, ArgumentsSignature signature, GetNamesAttributeNode getNamesNode) {
        FoldedNames foldedNames = new FoldedNames(new String[size]);
        int pos = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = getCast(i).valueProfile.profile(elements[i]);
            pos += processNamesElement(naNameBranch, foldedNames, pos, element, i, signature, getNamesNode);
        }
        RStringVector result = RDataFactory.createStringVector(foldedNames.names, foldedNames.complete);
        result.incRefCount();
        return result;
    }

    private int processNamesElement(BranchProfile naNameBranch, FoldedNames foldedNames, int pos, Object element,
                    int index, ArgumentsSignature signature,
                    GetNamesAttributeNode getNamesNode) {
        String signatureName = signature.getName(index);
        if (element instanceof RAbstractVector) {
            RAbstractVector v = (RAbstractVector) element;
            int length = v.getLength();

            RStringVector newNames = getNamesNode.getNames(v);
            if (signatureName != null && length > 0) {
                if (fastNamesMerge.profile(length == 1 && newNames == null)) {
                    newNames = RDataFactory.createStringVector(new String[]{signatureName}, true);
                } else {
                    newNames = RDataFactory.createStringVector(mergeNamesSlow(length, signatureName, newNames), true);
                }
            }
            if (hasNewNamesProfile.profile(newNames != null)) {
                VectorAccess newNamesAccess = newNames.slowPathAccess();
                try (VectorAccess.RandomIterator newNamesIter = newNamesAccess.randomAccess(newNames)) {
                    for (int i1 = 0; i1 < length; i1++) {
                        foldedNames.names[pos + i1] = newNamesAccess.getString(newNamesIter, i1);
                    }
                    if (!newNames.isComplete()) {
                        naNameBranch.enter();
                        foldedNames.complete = false;
                    }
                }
            } else {
                for (int i1 = 0; i1 < length; i1++) {
                    foldedNames.names[pos + i1] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                }
            }
            return v.getLength();
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            String name = signatureName != null ? signatureName : RRuntime.NAMES_ATTR_EMPTY_VALUE;
            foldedNames.names[pos] = name;
            return 1;
        }
    }

    @ExplodeLoop
    private RAbstractVector foldContents(int cachedPrecedence, Object[] elements, int size, RStringVector namesVector) {
        RAbstractVector result = createResultVector(cachedPrecedence, size, namesVector);
        int pos = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = getCast(i).valueProfile.profile(elements[i]);
            pos += processContentElement(result, pos, element, i);
        }
        return result;
    }

    private int processContentElement(RAbstractVector result, int pos, Object element, int elementIndex) {
        if (isAbstractVectorProfile.profile(element instanceof RAbstractVector)) {
            RAbstractVector v = (RAbstractVector) element;
            VectorDataLibrary resultDataLib = getVectorDataLibrary();
            VectorDataLibrary vDataLib = getElemDataLib(elementIndex);
            Object resultData = result.getData();
            Object vData = v.getData();
            try (RandomAccessWriteIterator resultIt = resultDataLib.randomAccessWriteIterator(resultData)) {
                RandomAccessIterator vIt = vDataLib.randomAccessIterator(vData);
                for (int i = 0; i < v.getLength(); i++) {
                    resultDataLib.transfer(resultData, resultIt, pos + i, vDataLib, vIt, vData, i);
                }
                resultDataLib.commitRandomAccessWriteIterator(resultData, resultIt, vDataLib.getNACheck(vData).neverSeenNA());
            }
            return vDataLib.getLength(vData);
        } else if (element instanceof RNull) {
            // nothing to do - NULL elements are skipped
            return 0;
        } else {
            getVectorDataLibrary().setDataAtAsObject(result.getData(), pos, element);
            return 1;
        }
    }

    private VectorDataLibrary getVectorDataLibrary() {
        if (vectorDataLibrary == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            vectorDataLibrary = insert(VectorDataLibrary.getFactory().createDispatched(DSLConfig.getGenericVectorAccessCacheSize()));
        }
        return vectorDataLibrary;
    }

    private static boolean signatureHasNames(ArgumentsSignature signature) {
        return signature != null && signature.getNonNullCount() > 0;
    }

    @Specialization(guards = "!isArguments(args)")
    protected Object nonArguments(Object args, boolean recursive,
                    @Cached("create()") Combine combine) {
        return combine.executeCombine(new RArgsValuesAndNames(new Object[]{args}, EMPTY_SIGNATURE), recursive);
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

    private static RAbstractVector createResultVector(int precedence, int size, RStringVector names) {
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
                return RDataFactory.createExpression(size, names);
            case LIST_PRECEDENCE:
                return RDataFactory.createList(size, names);
            case NO_PRECEDENCE:
                return null;
            default:
                throw RError.nyi(null, "unsupported combine type");
        }
    }

    protected static final class GenericCastNode extends TruffleBoundaryNode {
        @Child private CastNode cachedCastNode;
        private int cachedPrecedence = NO_PRECEDENCE;

        protected static GenericCastNode create() {
            return new GenericCastNode();
        }

        private CastNode get(int precedence) {
            CompilerAsserts.neverPartOfCompilation();
            if (precedence == NO_PRECEDENCE) {
                return null;
            }
            CastNode current = cachedCastNode;
            if (current == null || cachedPrecedence != precedence) {
                cachedPrecedence = precedence;
                return cachedCastNode = insert(createCast(precedence));
            }
            return current;
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

    protected final class CombineInputCast extends Node {

        @Child private ExtractDimNamesAttributeNode extractDimNamesNode;
        @Child private ExtractNamesAttributeNode extractNamesNode;

        private final ValueProfile valueProfile = ValueProfile.createClassProfile();
        private final ValueProfile inputValueProfile = ValueProfile.createClassProfile();

        @CompilationFinal private ConditionProfile hasNamesProfile;
        @CompilationFinal private ConditionProfile hasDimNamesProfile;

        protected boolean isMaterializedVector(Object value, int elementIndex) {
            if (!(value instanceof RAbstractVector)) {
                return false;
            }
            return getElemDataLib(elementIndex).isWriteable(((RAbstractVector) value).getData());
        }

        public Object cast(Object operand, int elementIndex) {
            Object profiled = inputValueProfile.profile(operand);
            if (isMaterializedVector(profiled, elementIndex)) {
                RAbstractVector vector = (RAbstractVector) profiled;
                if (vector.getAttributes() != null) {
                    if (extractNamesNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        extractNamesNode = insert(ExtractNamesAttributeNode.create());
                    }
                    if (hasNamesProfile == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasNamesProfile = ConditionProfile.createBinaryProfile();
                    }
                    RStringVector vecNames = extractNamesNode.execute(vector);
                    if (hasNamesProfile.profile(vecNames != null)) {
                        RAbstractVector result = vector.copyDropAttributes();
                        result.initAttributes(RAttributesLayout.createNames(vecNames));
                        return result;
                    } else {
                        if (extractDimNamesNode == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            extractDimNamesNode = insert(ExtractDimNamesAttributeNode.create());
                        }
                        if (hasDimNamesProfile == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            hasDimNamesProfile = ConditionProfile.createBinaryProfile();
                        }
                        RList dimNames = extractDimNamesNode.execute(vector);
                        if (hasDimNamesProfile.profile(dimNames != null)) {
                            RAbstractVector result = vector.copyDropAttributes();
                            result.initAttributes(RAttributesLayout.createDimNames(dimNames));
                            return result;
                        }
                    }
                }
            }
            return profiled;
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.NONE;
        }
    }
}
