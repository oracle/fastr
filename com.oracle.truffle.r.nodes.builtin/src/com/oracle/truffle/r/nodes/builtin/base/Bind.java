/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode;
import com.oracle.truffle.r.nodes.function.UseMethodInternalNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastListNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.PrecedenceNode;
import com.oracle.truffle.r.nodes.unary.PrecedenceNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class Bind extends RBaseNode {

    protected enum BindType {
        rbind,
        cbind;
    }

    protected static final int NO_PRECEDENCE = PrecedenceNode.NO_PRECEDENCE;
    protected static final int RAW_PRECEDENCE = PrecedenceNode.RAW_PRECEDENCE;
    protected static final int LOGICAL_PRECEDENCE = PrecedenceNode.LOGICAL_PRECEDENCE;
    protected static final int INT_PRECEDENCE = PrecedenceNode.INT_PRECEDENCE;
    protected static final int DOUBLE_PRECEDENCE = PrecedenceNode.DOUBLE_PRECEDENCE;
    protected static final int COMPLEX_PRECEDENCE = PrecedenceNode.COMPLEX_PRECEDENCE;
    protected static final int STRING_PRECEDENCE = PrecedenceNode.STRING_PRECEDENCE;
    protected static final int LIST_PRECEDENCE = PrecedenceNode.LIST_PRECEDENCE;
    protected static final int EXPRESSION_PRECEDENCE = PrecedenceNode.EXPRESSION_PRECEDENCE;

    public abstract Object execute(VirtualFrame frame, int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, int precedence);

    @Child private CastToVectorNode castVector;
    @Child private UseMethodInternalNode dcn;
    @Child private CastLogicalNode castLogical;

    private final BindType type;

    private final ConditionProfile nullNamesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile emptyVectorProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nonNullNames = BranchProfile.create();
    private final NACheck naCheck = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    protected final ValueProfile resultProfile = ValueProfile.createClassProfile();
    protected final ValueProfile vectorProfile = ValueProfile.createClassProfile();

    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();

    protected int precedence(RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getArguments();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], false));
        }
        return precedence;
    }

    protected Bind(BindType type) {
        this.type = type;
    }

    protected RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return (RAbstractVector) castVector.execute(value);
    }

    protected Object castLogical(Object operand, boolean preserveAllAttr) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castLogical.execute(operand);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "precedence == NO_PRECEDENCE")
    protected RNull allNull(VirtualFrame frame, int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, int precedence) {
        return RNull.instance;
    }

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get("deparse.level", "...");

    private static final RStringVector DATA_FRAME_CLASS = RDataFactory.createStringVectorFromScalar("data.frame");

    @Specialization(guards = {"args.length > 1", "isDataFrame(args)"})
    protected Object allDataFrame(VirtualFrame frame, int deparseLevel, @SuppressWarnings("unused") Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(new UseMethodInternalNode(type.toString(), SIGNATURE, false));
        }
        try {
            return dcn.execute(frame, DATA_FRAME_CLASS, new Object[]{deparseLevel, promiseArgs});
        } catch (S3FunctionLookupNode.NoGenericMethodException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    private Object bindInternal(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, CastNode castNode, boolean needsVectorCast) {
        ArgumentsSignature signature = promiseArgs.getSignature();
        String[] vecNames = nullNamesProfile.profile(signature.getNonNullCount() == 0) ? null : new String[signature.getLength()];
        RAbstractVector[] vectors = new RAbstractVector[args.length];
        boolean complete = true;
        int ind = 0;
        naCheck.enable(true);
        for (int i = 0; i < args.length; i++) {
            if (vecNames != null) {
                nonNullNames.enter();
                vecNames[ind] = signature.getName(i);
                naCheck.check(vecNames[ind]);
            }
            Object result = castNode.execute(args[i]);
            RAbstractVector vector;
            if (needsVectorCast) {
                vector = castVector(result);
            } else {
                vector = (RAbstractVector) result;
            }
            if (emptyVectorProfile.profile(vector.getLength() == 0)) {
                // nothing to do
            } else {
                vectors[ind] = vector;
                complete &= vector.isComplete();
                ind++;
            }
        }
        if (emptyVectorProfile.profile(ind < args.length)) {
            if (vecNames != null) {
                nonNullNames.enter();
                vecNames = Arrays.copyOf(vecNames, ind);
            }
            vectors = Arrays.copyOf(vectors, ind);
        }
        if (type == BindType.cbind) {
            return genericCBind(promiseArgs, vectors, complete, vecNames, naCheck.neverSeenNA(), deparseLevel);
        } else {
            return genericRBind(promiseArgs, vectors, complete, vecNames, naCheck.neverSeenNA(), deparseLevel);
        }
    }

    @Specialization(guards = {"precedence == INT_PRECEDENCE", "args.length > 1", "!isDataFrame(args)"})
    protected Object allInt(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence, //
                    @Cached("create()") CastIntegerNode cast) {
        return bindInternal(deparseLevel, args, promiseArgs, cast, true);
    }

    @Specialization(guards = {"precedence == DOUBLE_PRECEDENCE", "args.length > 1", "!isDataFrame(args)"})
    protected Object allDouble(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence, //
                    @Cached("create()") CastDoubleNode cast) {
        return bindInternal(deparseLevel, args, promiseArgs, cast, true);
    }

    @Specialization(guards = {"precedence == STRING_PRECEDENCE", "args.length> 1", "!isDataFrame(args)"})
    protected Object allString(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence, //
                    @Cached("create()") CastStringNode cast) {
        return bindInternal(deparseLevel, args, promiseArgs, cast, true);
    }

    @Specialization(guards = {"precedence == COMPLEX_PRECEDENCE", "args.length > 1", "!isDataFrame(args)"})
    protected Object allComplex(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence, //
                    @Cached("create()") CastComplexNode cast) {
        return bindInternal(deparseLevel, args, promiseArgs, cast, true);
    }

    @Specialization(guards = {"precedence == LIST_PRECEDENCE", "args.length > 1", "!isDataFrame(args)"})
    protected Object allList(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence, //
                    @Cached("create()") CastListNode cast) {
        return bindInternal(deparseLevel, args, promiseArgs, cast, false);
    }

    /**
     * Compute dimnames for rows (cbind) or columns (rbind) from names of elements of combined
     * vectors.
     *
     * @param vec
     * @param dimLength
     * @return dimnames
     */
    protected Object getDimResultNamesFromElements(RAbstractVector vec, int dimLength, int dimInd) {
        Object firstDimResultNames = RNull.instance;
        Object firstDimNames = RNull.instance;
        if (vec.isMatrix()) {
            RList vecDimNames = vec.getDimNames(attrProfiles);
            if (vecDimNames != null) {
                firstDimNames = vecDimNames.getDataAt(dimInd);
            }
        } else if (!vec.isArray() || vec.getDimensions().length == 1) {
            RStringVector names = vec.getNames(attrProfiles);
            firstDimNames = names == null ? RNull.instance : names;
        } else {
            RInternalError.unimplemented("binding multi-dimensional arrays is not supported");
        }
        if (firstDimNames != RNull.instance) {
            RStringVector names = (RStringVector) firstDimNames;
            if (names != null && names.getLength() == dimLength) {
                firstDimResultNames = names;
            }
        }
        return firstDimResultNames;
    }

    /**
     * Compute dimnames for columns (cbind) or rows (rbind) from names of vectors being combined or
     * by deparsing.
     */
    protected int getDimResultNamesFromVectors(RArgsValuesAndNames promiseArgs, RAbstractVector vec, String[] argNames, int resDim, int oldInd, int vecInd, int deparseLevel,
                    String[] dimNamesArray,
                    int dimNamesInd) {
        int ind = oldInd;
        if (vec.isMatrix()) {
            RList vecDimNames = vec.getDimNames(attrProfiles);
            if (vecDimNames != null) {
                Object resDimNames = vecDimNames.getDataAt(dimNamesInd);
                if (resDimNames != RNull.instance) {
                    if (resDimNames instanceof String) {
                        dimNamesArray[ind++] = (String) resDimNames;
                    } else {
                        RStringVector names = (RStringVector) resDimNames;
                        assert names.getLength() == resDim;
                        for (int i = 0; i < names.getLength(); i++) {
                            dimNamesArray[ind++] = names.getDataAt(i);
                        }
                    }
                    return ind;
                }
            }
            for (int i = 0; i < resDim; i++) {
                dimNamesArray[ind++] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
            }
            return -ind;
        } else if (!vec.isArray() || vec.getDimensions().length == 1) {
            if (argNames == null) {
                if (deparseLevel == 0) {
                    dimNamesArray[ind++] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                    return -ind;
                } else {
                    String deparsedName = deparseArgName(promiseArgs, deparseLevel, vecInd);
                    dimNamesArray[ind++] = deparsedName;
                    return deparsedName == RRuntime.NAMES_ATTR_EMPTY_VALUE ? -ind : ind;
                }
            } else {
                if (argNames[vecInd] == null) {
                    dimNamesArray[ind++] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                    return -ind;
                } else {
                    dimNamesArray[ind++] = argNames[vecInd];
                    return ind;
                }
            }
        } else {
            RInternalError.unimplemented("binding multi-dimensional arrays is not supported");
            return 0;
        }
    }

    /**
     *
     * @param vectors vectors to be combined
     * @param res result dims
     * @param bindDims columns dim (cbind) or rows dim (rbind)
     * @return whether number of rows (cbind) or columns (rbind) in vectors is the same
     */
    protected boolean getResultDimensions(RAbstractVector[] vectors, int[] res, int[] bindDims) {
        int srcDim1Ind = type == BindType.cbind ? 0 : 1;
        int srcDim2Ind = type == BindType.cbind ? 1 : 0;
        assert vectors.length > 0;
        int[] dim = getDimensions(vectors[0]);
        assert dim.length == 2;
        bindDims[0] = dim[srcDim2Ind];
        res[srcDim1Ind] = dim[srcDim1Ind];
        res[srcDim2Ind] = dim[srcDim2Ind];
        boolean notEqualDims = false;
        for (int i = 1; i < vectors.length; i++) {
            int[] dims = getDimensions(vectors[i]);
            assert dims.length == 2;
            bindDims[i] = dims[srcDim2Ind];
            if (dims[srcDim1Ind] != res[srcDim1Ind]) {
                notEqualDims = true;
                if (dims[srcDim1Ind] > res[srcDim1Ind]) {
                    res[srcDim1Ind] = dims[srcDim1Ind];
                }
            }
            res[srcDim2Ind] += dims[srcDim2Ind];
        }
        return notEqualDims;
    }

    protected int[] getDimensions(RAbstractVector vector) {
        int[] dimensions = vector.getDimensions();
        if (dimensions == null || dimensions.length != 2) {
            return type == BindType.cbind ? new int[]{vector.getLength(), 1} : new int[]{1, vector.getLength()};
        } else {
            assert dimensions.length == 2;
            return dimensions;
        }
    }

    @TruffleBoundary
    protected static String deparseArgName(RArgsValuesAndNames promiseArgs, int deparseLevel, int argInd) {
        assert promiseArgs.getLength() >= argInd;
        Object argValue = promiseArgs.getArgument(argInd);
        if (argValue instanceof RPromise) {
            RPromise p = (RPromise) argValue;
            Object node = RASTUtils.createLanguageElement(p.getRep().asRSyntaxNode());
            if (deparseLevel == 1 && node instanceof RSymbol) {
                return ((RSymbol) node).toString();
            } // else - TODO handle deparseLevel > 1
        }
        // else - TODO handle non-promise arg (particularly a problem with the bind function
        // execuded via do.call

        return RRuntime.NAMES_ATTR_EMPTY_VALUE;
    }

    @Child private InheritsCheckNode inheritsCheck = new InheritsCheckNode(RRuntime.CLASS_DATA_FRAME);

    protected boolean isDataFrame(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (inheritsCheck.execute(args[i])) {
                return true;
            }
        }

        return false;
    }

    @RBuiltin(name = "cbind", kind = INTERNAL, parameterNames = {"deparse.level", "..."}, behavior = COMPLEX)
    public abstract static class CbindInternal extends RBuiltinNode {

        @Child private Bind bind = BindNodeGen.create(BindType.cbind);
        @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("deparse.level").asIntegerVector().findFirst();
        }

        private int precedence(Object[] args) {
            int precedence = -1;
            for (int i = 0; i < args.length; i++) {
                precedence = Math.max(precedence, precedenceNode.executeInteger(args[i], false));
            }
            return precedence;
        }

        @Specialization
        protected Object bind(VirtualFrame frame, int deparseLevel, RArgsValuesAndNames args) {
            return bind.execute(frame, deparseLevel, args.getArguments(), (RArgsValuesAndNames) RArguments.getArgument(frame, 0), precedence(args.getArguments()));
        }
    }

    private final BranchProfile everSeenNotEqualRows = BranchProfile.create();
    private final BranchProfile everSeenNotEqualColumns = BranchProfile.create();

    @Specialization(guards = {"precedence != NO_PRECEDENCE", "args.length == 1"})
    protected Object allOneElem(int deparseLevel, Object[] args, RArgsValuesAndNames promiseArgs, @SuppressWarnings("unused") int precedence) {
        RAbstractVector vec = castVector(args[0]);
        if (vec.isMatrix()) {
            return vec;
        }
        int[] dims = getDimensions(vec);
        // for cbind dimNamesA is names for the 1st dim and dimNamesB is names for 2nd dim; for
        // rbind the other way around
        Object dimNamesA = vec.getNames(attrProfiles) == null ? RNull.instance : vec.getNames(attrProfiles);
        Object dimNamesB;

        ArgumentsSignature signature = promiseArgs.getSignature();
        if (signature.getNonNullCount() == 0) {
            if (deparseLevel == 0) {
                dimNamesB = RNull.instance;
            } else {
                // var arg is at the first position - as in the R bind call
                String deparsedName = deparseArgName(promiseArgs, deparseLevel, 0);
                dimNamesB = deparsedName == RRuntime.NAMES_ATTR_EMPTY_VALUE ? RNull.instance : RDataFactory.createStringVector(deparsedName);
            }
        } else {
            String[] names = new String[signature.getLength()];
            for (int i = 0; i < names.length; i++) {
                names[i] = signature.getName(i);
            }
            dimNamesB = RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR);
        }

        RVector res = (RVector) vec.copyWithNewDimensions(dims);
        res.setDimNames(RDataFactory.createList(type == BindType.cbind ? new Object[]{dimNamesA, dimNamesB} : new Object[]{dimNamesB, dimNamesA}));
        res.copyRegAttributesFrom(vec);
        return res;
    }

    public RVector genericCBind(RArgsValuesAndNames promiseArgs, RAbstractVector[] vectors, boolean complete, String[] vecNames, boolean vecNamesComplete, int deparseLevel) {

        int[] resultDimensions = new int[2];
        int[] secondDims = new int[vectors.length];
        boolean notEqualRows = getResultDimensions(vectors, resultDimensions, secondDims);
        RVector result = resultProfile.profile(vectors[0].createEmptySameType(resultDimensions[0] * resultDimensions[1], complete));

        int ind = 0;
        Object rowDimResultNames = RNull.instance;
        String[] colDimNamesArray = new String[resultDimensions[1]];
        int colInd = 0;
        boolean allColDimNamesNull = true;
        for (int i = 0; i < vectors.length; i++) {
            RAbstractVector vec = vectorProfile.profile(vectors[i]);
            if (rowDimResultNames == RNull.instance) {
                // get the first valid names value
                rowDimResultNames = getDimResultNamesFromElements(vec, resultDimensions[0], 0);
            }

            // compute dimnames for the second dimension
            int newColInd = getDimResultNamesFromVectors(promiseArgs, vec, vecNames, secondDims[i], colInd, i, deparseLevel, colDimNamesArray, 1);
            if (newColInd < 0) {
                colInd = -newColInd;
            } else {
                allColDimNamesNull = false;
                colInd = newColInd;
            }

            // compute result vector values
            int vecLength = vec.getLength();
            for (int j = 0; j < vecLength; j++) {
                result.transferElementSameType(ind++, vec, j);
            }
            if (notEqualRows) {
                everSeenNotEqualRows.enter();
                if (vecLength < resultDimensions[0]) {
                    // re-use vector elements
                    int k = 0;
                    for (int j = 0; j < resultDimensions[0] - vecLength; j++, k = Utils.incMod(k, vecLength)) {
                        result.transferElementSameType(ind++, vectors[i], k);
                    }

                    if (k != 0) {
                        RError.warning(this, RError.Message.ROWS_NOT_MULTIPLE, i + 1);
                    }
                }
            }

        }
        Object colDimResultNames = allColDimNamesNull ? RNull.instance : RDataFactory.createStringVector(colDimNamesArray, vecNamesComplete);
        result.setDimensions(resultDimensions);
        result.setDimNames(RDataFactory.createList(new Object[]{rowDimResultNames, colDimResultNames}));
        return result;
    }

    @RBuiltin(name = "rbind", kind = INTERNAL, parameterNames = {"deparse.level", "..."}, behavior = COMPLEX)
    public abstract static class RbindInternal extends RBuiltinNode {

        @Child private Bind bind = BindNodeGen.create(BindType.rbind);
        @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("deparse.level").asIntegerVector().findFirst();
        }

        private int precedence(Object[] args) {
            int precedence = -1;
            for (int i = 0; i < args.length; i++) {
                precedence = Math.max(precedence, precedenceNode.executeInteger(args[i], false));
            }
            return precedence;
        }

        @Specialization
        protected Object bind(VirtualFrame frame, int deparseLevel, RArgsValuesAndNames args) {
            return bind.execute(frame, deparseLevel, args.getArguments(), (RArgsValuesAndNames) RArguments.getArgument(frame, 0), precedence(args.getArguments()));
        }
    }

    public RVector genericRBind(RArgsValuesAndNames promiseArgs, RAbstractVector[] vectors, boolean complete, String[] vecNames, boolean vecNamesComplete, int deparseLevel) {

        int[] resultDimensions = new int[2];
        int[] firstDims = new int[vectors.length];
        boolean notEqualColumns = getResultDimensions(vectors, resultDimensions, firstDims);
        RVector result = resultProfile.profile(vectors[0].createEmptySameType(resultDimensions[0] * resultDimensions[1], complete));

        Object colDimResultNames = RNull.instance;
        String[] rowDimNamesArray = new String[resultDimensions[0]];
        int rowInd = 0;
        boolean allRowDimNamesNull = true;
        int dstRowInd = 0;
        for (int i = 0; i < vectors.length; i++) {
            RAbstractVector vec = vectorProfile.profile(vectors[i]);
            if (colDimResultNames == RNull.instance) {
                // get the first valid names value
                colDimResultNames = getDimResultNamesFromElements(vec, resultDimensions[1], 1);
            }

            // compute dimnames for the second dimension
            int newRowInd = getDimResultNamesFromVectors(promiseArgs, vec, vecNames, firstDims[i], rowInd, i, deparseLevel, rowDimNamesArray, 0);
            if (newRowInd < 0) {
                rowInd = -newRowInd;
            } else {
                allRowDimNamesNull = false;
                rowInd = newRowInd;
            }

            // compute result vector values
            int vecLength = vec.getLength();
            int srcInd = 0;
            int j = 0;
            for (; j < vecLength / firstDims[i]; j++) {
                for (int k = dstRowInd; k < dstRowInd + firstDims[i]; k++) {
                    result.transferElementSameType(j * resultDimensions[0] + k, vec, srcInd++);
                }
            }
            if (notEqualColumns) {
                everSeenNotEqualColumns.enter();
                if (j < resultDimensions[1]) {
                    // re-use vector elements
                    int k = 0;
                    for (; j < resultDimensions[1]; j++, k = Utils.incMod(k, vecLength % resultDimensions[1])) {
                        result.transferElementSameType(j * resultDimensions[0] + (dstRowInd + firstDims[i] - 1), vectors[i], k);
                    }

                    if (k != 0) {
                        RError.warning(this, RError.Message.COLUMNS_NOT_MULTIPLE, i + 1);
                    }
                }
            }
            dstRowInd += firstDims[i];

        }
        Object rowDimResultNames = allRowDimNamesNull ? RNull.instance : RDataFactory.createStringVector(rowDimNamesArray, vecNamesComplete);
        result.setDimensions(resultDimensions);
        result.setDimNames(RDataFactory.createList(new Object[]{rowDimResultNames, colDimResultNames}));
        return result;
    }
}
