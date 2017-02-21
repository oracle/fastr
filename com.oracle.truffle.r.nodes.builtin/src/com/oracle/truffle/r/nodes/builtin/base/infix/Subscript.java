/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.convertIndex;
import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.profile;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ProfiledValue;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.SubscriptSpecial2Common;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.SubscriptSpecialCommon;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Subscript code for vectors minus list is the same as subset code, this class allows sharing it.
 */
@NodeChild(value = "vector", type = ProfiledValue.class)
@NodeChild(value = "index", type = ConvertIndex.class)
abstract class SubscriptSpecialBase extends SubscriptSpecialCommon {

    protected SubscriptSpecialBase(boolean inReplacement) {
        super(inReplacement);
    }

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    protected boolean simpleVector(RAbstractVector vector) {
        return classHierarchy.execute(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected int access(RAbstractIntVector vector, int index) {
        return vector.getDataAt(index - 1);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected double access(RAbstractDoubleVector vector, int index) {
        return vector.getDataAt(index - 1);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)"})
    protected String access(RAbstractStringVector vector, int index) {
        return vector.getDataAt(index - 1);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}

/**
 * Subscript code for matrices minus list is the same as subset code, this class allows sharing it.
 */
@NodeChild(value = "vector", type = ProfiledValue.class)
@NodeChild(value = "index1", type = ConvertIndex.class)
@NodeChild(value = "index2", type = ConvertIndex.class)
abstract class SubscriptSpecial2Base extends SubscriptSpecial2Common {

    protected SubscriptSpecial2Base(boolean inReplacement) {
        super(inReplacement);
    }

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    protected abstract ProfiledValue getVector();

    protected abstract ConvertIndex getIndex1();

    protected abstract ConvertIndex getIndex2();

    protected boolean simpleVector(RAbstractVector vector) {
        return classHierarchy.execute(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected int access(RAbstractIntVector vector, int index1, int index2) {
        return vector.getDataAt(matrixIndex(vector, index1, index2));
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected double access(RAbstractDoubleVector vector, int index1, int index2) {
        return vector.getDataAt(matrixIndex(vector, index1, index2));
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)"})
    protected String access(RAbstractStringVector vector, int index1, int index2) {
        return vector.getDataAt(matrixIndex(vector, index1, index2));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index1, Object index2) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}

abstract class SubscriptSpecial extends SubscriptSpecialBase {

    protected SubscriptSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)", "!inReplacement"})
    protected static Object access(RList vector, int index,
                    @Cached("create()") ExtractListElement extract) {
        return extract.execute(vector, index - 1);
    }

    protected static ExtractVectorNode createAccess() {
        return ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);
    }

    @Specialization(guards = {"simpleVector(vector)", "!inReplacement"})
    protected static Object access(VirtualFrame frame, RAbstractVector vector, Object index,
                    @Cached("createAccess()") ExtractVectorNode extract) {
        return extract.apply(frame, vector, new Object[]{index}, RRuntime.LOGICAL_TRUE, RLogical.TRUE);
    }
}

abstract class SubscriptSpecial2 extends SubscriptSpecial2Base {

    protected SubscriptSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)", "!inReplacement"})
    protected Object access(RList vector, int index1, int index2,
                    @Cached("create()") ExtractListElement extract) {
        return extract.execute(vector, matrixIndex(vector, index1, index2));
    }
}

@RBuiltin(name = "[[", kind = PRIMITIVE, parameterNames = {"x", "...", "exact", "drop"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
@TypeSystemReference(RTypes.class)
public abstract class Subscript extends RBuiltinNode {

    @RBuiltin(name = ".subset2", kind = PRIMITIVE, parameterNames = {"x", "...", "exact", "drop"}, behavior = PURE)
    public abstract class DefaultBuiltin {
        // same implementation as "[[", with different dispatch
    }

    static {
        Casts.noCasts(Subscript.class);
    }

    public static RNode special(ArgumentsSignature signature, RNode[] arguments, boolean inReplacement) {
        if (signature.getNonNullCount() == 0) {
            if (arguments.length == 2) {
                return SubscriptSpecialNodeGen.create(inReplacement, profile(arguments[0]), convertIndex(arguments[1]));
            } else if (arguments.length == 3) {
                return SubscriptSpecial2NodeGen.create(inReplacement, profile(arguments[0]), convertIndex(arguments[1]), convertIndex(arguments[2]));
            }
        }
        return null;
    }

    @Child private ExtractVectorNode extractNode = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE};
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull getNoInd(RNull x, Object inds, Object exactVec, Object drop) {
        return x;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "indexes.isEmpty()")
    protected Object getNoInd(Object x, RArgsValuesAndNames indexes, RAbstractLogicalVector exact, RAbstractLogicalVector drop) {
        throw RError.error(this, RError.Message.NO_INDEX);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object get(Object x, RMissing indexes, RAbstractLogicalVector exact, RAbstractLogicalVector drop) {
        throw RError.error(this, RError.Message.NO_INDEX);
    }

    @Specialization(guards = "!indexes.isEmpty()")
    protected Object get(VirtualFrame frame, Object x, RArgsValuesAndNames indexes, RAbstractLogicalVector exact, @SuppressWarnings("unused") Object drop) {
        /*
         * "drop" is not actually used by this builtin, but it needs to be in the argument list
         * (because the "drop" argument needs to be skipped).
         */
        return extractNode.apply(frame, x, indexes.getArguments(), exact, RLogical.TRUE);
    }
}
