/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "inherits", kind = INTERNAL)
public abstract class Inherits extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "what", "which"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    public abstract byte execute(VirtualFrame frame, Object x, RAbstractStringVector what, byte which);

    @SlowPath
    // map operations lead to recursion resulting in compilation failure
    @Specialization(order = 0)
    public Object doesInherit(RAbstractVector x, RAbstractStringVector what, byte which) {
        controlVisibility();
        boolean isWhich = which == RRuntime.LOGICAL_TRUE;
        RStringVector klass = x.getClassHierarchy();

        // Create a mapping for elements to their respective positions
        // in the vector for faster lookup.
        Map<String, Integer> classToPos = new HashMap<>();
        for (int i = 0; i < klass.getLength(); ++i) {
            classToPos.put(klass.getDataAt(i), i);
        }
        if (isWhich) {
            int[] result = new int[what.getLength()];
            for (int i = 0; i < what.getLength(); ++i) {
                final Integer pos = classToPos.get(what.getDataAt(i));
                if (pos == null) {
                    result[i] = 0;
                } else {
                    result[i] = pos + 1;
                }
            }
            return RDataFactory.createIntVector(result, true);
        } else {
            for (int i = 0; i < what.getLength(); ++i) {
                if (classToPos.get(what.getDataAt(i)) != null) {
                    return RRuntime.LOGICAL_TRUE;
                }
            }
            return RRuntime.LOGICAL_FALSE;
        }
    }

    // TODO: these generic specializations must go away - this simply does not work in general (e.g.
    // inherits is used by implementation of is.factor, which means that arguments of different
    // types can easily flow through the same node)

// @Specialization(order = 3)
// @SuppressWarnings("unused")
// public Object doesInherit(RAbstractVector x, RAbstractStringVector what, Object which) {
// controlVisibility();
// CompilerDirectives.transferToInterpreter();
// throw RError.getNotLengthOneLogicalVector(getEncapsulatingSourceSection(), RRuntime.WHICH);
// }
//
// @Specialization(order = 4)
// @SuppressWarnings("unused")
// public Object doesInherit(RAbstractVector x, Object what, Object which) {
// controlVisibility();
// CompilerDirectives.transferToInterpreter();
// throw RError.getNotCharacterVector(getEncapsulatingSourceSection(), RRuntime.WHAT);
// }
//
// @Specialization(order = 6)
// @SuppressWarnings("unused")
// public Object doesInherit(Object x, Object what, Object which) {
// controlVisibility();
// throw new UnsupportedOperationException();
// }
}
