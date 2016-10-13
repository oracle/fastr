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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.SubscriptSpecialCommon;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RSpecialFactory;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@NodeChild(value = "arguments", type = RNode[].class)
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
abstract class UpdateSubscriptSpecial extends SubscriptSpecialCommon {
    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);
    private final NACheck naCheck = NACheck.create();

    protected boolean simple(Object vector) {
        return classHierarchy.execute(vector) == null;
    }

    /**
     * Checks if the value is single element that can be put into a list or vector as is, because in
     * the case of vectors on the LSH of update we take each element and put it into the RHS of the
     * update function.
     */
    protected static boolean isSingleElement(Object value) {
        return value instanceof Integer || value instanceof Double || value instanceof Byte || value instanceof String;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RIntVector access(RIntVector vector, int index, int value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RDoubleVector access(RDoubleVector vector, int index, double value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RStringVector access(RStringVector vector, int index, String value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "isValidIndex(list, index)", "isSingleElement(value)"})
    protected static Object access(RList list, int index, Object value) {
        list.setDataAt(list.getInternalStore(), index - 1, value);
        return list;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidDoubleIndex(vector, index)"})
    protected RIntVector accessDoubleIndex(RIntVector vector, double index, int value) {
        return vector.updateDataAt(toIndex(index) - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidDoubleIndex(vector, index)"})
    protected RDoubleVector accessDoubleIndex(RDoubleVector vector, double index, double value) {
        return vector.updateDataAt(toIndex(index) - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidDoubleIndex(vector, index)"})
    protected RStringVector accessDoubleIndex(RStringVector vector, double index, String value) {
        return vector.updateDataAt(toIndex(index) - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "isValidDoubleIndex(list, index)", "isSingleElement(value)"})
    protected Object accessDoubleIndex(RList list, double index, Object value) {
        list.setDataAt(list.getInternalStore(), toIndex(index) - 1, value);
        return list;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object access(Object vector, Object index, Object value) {
        throw RSpecialFactory.throwFullCallNeeded();
    }
}

@RBuiltin(name = "[[<-", kind = PRIMITIVE, parameterNames = {"", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateSubscript extends RBuiltinNode {

    @Child private ReplaceVectorNode replaceNode = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, false);

    private final ConditionProfile argsLengthLargerThanOneProfile = ConditionProfile.createBinaryProfile();

    public static RNode special(ArgumentsSignature signature, RNode[] arguments) {
        return SpecialsUtils.isCorrectUpdateSignature(signature) && arguments.length == 3 ? UpdateSubscriptSpecialNodeGen.create(arguments) : null;
    }

    @Specialization(guards = "!args.isEmpty()")
    protected Object update(VirtualFrame frame, Object x, RArgsValuesAndNames args) {
        Object value = args.getArgument(args.getLength() - 1);
        Object[] pos;
        if (argsLengthLargerThanOneProfile.profile(args.getLength() > 1)) {
            pos = Arrays.copyOf(args.getArguments(), args.getLength() - 1);
        } else {
            pos = new Object[]{RMissing.instance};
        }
        return replaceNode.apply(frame, x, pos, value);
    }

    @Specialization(guards = "args.isEmpty()")
    @SuppressWarnings("unused")
    protected Object getNoInd(Object x, RArgsValuesAndNames args) {
        throw RError.error(this, RError.Message.INVALID_ARG_NUMBER, "SubAssignArgs");
    }
}
