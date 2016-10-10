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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.RCallSpecialNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@NodeChild(value = "arguments", type = RNode[].class)
abstract class UpdateSubsetSpecial extends RNode {
    @Child private ClassHierarchyNode classHierarchy = ClassHierarchyNodeGen.create(false, false);
    private final NACheck naCheck = NACheck.create();

    protected boolean simple(Object vector) {
        return classHierarchy.execute(vector) == null;
    }

    protected static boolean inIntRange(RAbstractVector vector, int index) {
        return index >= 1 && index <= vector.getLength();
    }

    protected static boolean inDoubleRange(RAbstractVector vector, double index) {
        return index > 0 && index < (vector.getLength() + 1);
    }

    private static int toInt(double index) {
        int i = (int) index;
        return i == 0 ? 1 : i - 1;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inIntRange(vector, index)"})
    protected RIntVector access(RIntVector vector, int index, int value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inIntRange(vector, index)"})
    protected RDoubleVector access(RDoubleVector vector, int index, double value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inIntRange(vector, index)"})
    protected RStringVector access(RStringVector vector, int index, String value) {
        return vector.updateDataAt(index - 1, value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "inIntRange(list, index)"})
    protected static Object access(RList list, int index, Object value,
                    @Cached("create()") ShareObjectNode shareObject) {
        if (value instanceof RNull) {
            CompilerDirectives.transferToInterpreter();
            throw RCallSpecialNode.fullCallNeeded();
        }
        list.setDataAt(list.getInternalStore(), index - 1, shareObject.execute(value));
        return list;
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inDoubleRange(vector, index)"})
    protected RIntVector accessDoubleIndex(RIntVector vector, double index, int value) {
        return vector.updateDataAt(toInt(index), value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inDoubleRange(vector, index)"})
    protected RDoubleVector accessDoubleIndex(RDoubleVector vector, double index, double value) {
        return vector.updateDataAt(toInt(index), value, naCheck);
    }

    @Specialization(guards = {"simple(vector)", "!vector.isShared()", "inDoubleRange(vector, index)"})
    protected RStringVector accessDoubleIndex(RStringVector vector, double index, String value) {
        return vector.updateDataAt(toInt(index), value, naCheck);
    }

    @Specialization(guards = {"simple(list)", "!list.isShared()", "inDoubleRange(list, index)"})
    protected static Object accessDoubleIndex(RList list, double index, Object value,
                    @Cached("create()") ShareObjectNode shareObject) {
        if (value instanceof RNull) {
            CompilerDirectives.transferToInterpreter();
            throw RCallSpecialNode.fullCallNeeded();
        }
        list.setDataAt(list.getInternalStore(), toInt(index), shareObject.execute(value));
        return list;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object access(Object vector, Object index, Object value) {
        throw RCallSpecialNode.fullCallNeeded();
    }
}

@RBuiltin(name = "[<-", kind = PRIMITIVE, parameterNames = {"", "..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateSubset extends RBuiltinNode {

    private static final String valueArgName = "value".intern();

    @Child private ReplaceVectorNode replaceNode = ReplaceVectorNode.create(ElementAccessMode.SUBSET, false);
    private final ConditionProfile argsLengthLargerThanOneProfile = ConditionProfile.createBinaryProfile();

    @SuppressWarnings("StringEquality")
    public static RNode special(ArgumentsSignature signature, RNode[] arguments) {
        // Note: the signature names should be interned, the '!=' is intended
        boolean hasCorrectSignature = signature.getLength() != 3 || signature.getName(0) != null || signature.getName(1) != null || signature.getName(2) != valueArgName;
        return hasCorrectSignature && arguments.length == 3 ? UpdateSubsetSpecialNodeGen.create(arguments) : null;
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
