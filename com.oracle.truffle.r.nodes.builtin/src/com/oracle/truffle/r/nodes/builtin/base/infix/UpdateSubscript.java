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
import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.convertValue;
import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.profile;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ConvertValue;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ProfiledValue;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.SubscriptSpecial2Common;
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

@NodeChild(value = "vector", type = ProfiledValue.class)
@NodeChild(value = "index", type = ConvertIndex.class)
@NodeChild(value = "value", type = ConvertValue.class)
abstract class UpdateSubscriptSpecial extends SubscriptSpecialCommon {

    protected UpdateSubscriptSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    private final NACheck naCheck = NACheck.create();

    protected boolean simple(Object vector) {
        return classHierarchy.execute(vector) == null;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RIntVector set(RIntVector vector, int index, int value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RDoubleVector set(RDoubleVector vector, int index, double value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RStringVector set(RStringVector vector, int index, String value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "isValidIndex(list, index)", "isSingleElement(value)"})
    protected static Object set(RList list, int index, Object value) {
        list.setDataAt(list.getInternalStore(), index - 1, value);
        return list;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index)"})
    protected RDoubleVector setDoubleIntIndexIntValue(RDoubleVector vector, int index, int value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object setFallback(Object vector, Object index, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }
}

@NodeChild(value = "vector", type = ProfiledValue.class)
@NodeChild(value = "index1", type = ConvertIndex.class)
@NodeChild(value = "index2", type = ConvertIndex.class)
@NodeChild(value = "value", type = ConvertValue.class)
abstract class UpdateSubscriptSpecial2 extends SubscriptSpecial2Common {

    protected UpdateSubscriptSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);

    private final NACheck naCheck = NACheck.create();

    protected boolean simple(Object vector) {
        return classHierarchy.execute(vector) == null;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RIntVector set(RIntVector vector, int index1, int index2, int value) {
        return vector.updateDataAt(matrixIndex(vector, index1, index2), value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector set(RDoubleVector vector, int index1, int index2, double value) {
        return vector.updateDataAt(matrixIndex(vector, index1, index2), value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RStringVector set(RStringVector vector, int index1, int index2, String value) {
        return vector.updateDataAt(matrixIndex(vector, index1, index2), value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "isValidIndex(list, index1, index2)", "isSingleElement(value)"})
    protected Object set(RList list, int index1, int index2, Object value) {
        list.setDataAt(list.getInternalStore(), matrixIndex(list, index1, index2), value);
        return list;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "isValidIndex(vector, index1, index2)"})
    protected RDoubleVector setDoubleIntIndexIntValue(RDoubleVector vector, int index1, int index2, int value) {
        return vector.updateDataAt(matrixIndex(vector, index1, index2), value, naCheck);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object setFallback(Object vector, Object index1, Object index2, Object value) {
        throw RSpecialFactory.throwFullCallNeeded(value);
    }
}

@RBuiltin(name = "[[<-", kind = PRIMITIVE, parameterNames = {"", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateSubscript extends RBuiltinNode.Arg2 {

    @Child private ReplaceVectorNode replaceNode = ReplaceVectorNode.create(ElementAccessMode.SUBSCRIPT, false);

    private final ConditionProfile argsLengthLargerThanOneProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts.noCasts(UpdateSubscript.class);
    }

    public static RNode special(ArgumentsSignature signature, RNode[] args, boolean inReplacement) {
        if (SpecialsUtils.isCorrectUpdateSignature(signature) && (args.length == 3 || args.length == 4)) {
            ProfiledValue vector = profile(args[0]);
            ConvertIndex index = convertIndex(args[1]);
            if (args.length == 3) {
                return UpdateSubscriptSpecialNodeGen.create(inReplacement, vector, index, convertValue(args[2]));
            } else {
                return UpdateSubscriptSpecial2NodeGen.create(inReplacement, vector, index, convertIndex(args[2]), convertValue(args[3]));
            }
        }
        return null;
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
        throw error(RError.Message.INVALID_ARG_NUMBER, "SubAssignArgs");
    }
}
