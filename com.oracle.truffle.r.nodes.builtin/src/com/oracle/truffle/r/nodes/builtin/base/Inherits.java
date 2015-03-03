/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "inherits", kind = INTERNAL, parameterNames = {"x", "what", "which"})
// TODO inherits is applicable to every type of object, if only because of "try-error".
public abstract class Inherits extends RBuiltinNode {

    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private InheritsNode inheritsNode;

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    private InheritsNode initInheritsNode() {
        if (inheritsNode == null) {
            inheritsNode = insert(InheritsNodeGen.create(null, null));
        }
        return inheritsNode;
    }

    @SuppressWarnings("unused")
    public boolean whichFalse(RAbstractContainer x, RAbstractStringVector what, byte which) {
        return which != RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public boolean whichFalse(RConnection x, RAbstractStringVector what, byte which) {
        return which != RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public boolean whichFalse(RFunction x, RAbstractStringVector what, byte which) {
        return which != RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public boolean whichFalse(REnvironment x, RAbstractStringVector what, byte which) {
        return which != RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public boolean whichFalse(RSymbol x, RAbstractStringVector what, byte which) {
        return which != RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doesInherit(RNull x, RAbstractStringVector what, byte which) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "whichFalse")
    protected Object doesInherit(VirtualFrame frame, REnvironment x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(frame, x, what);
    }

    @Specialization(guards = "!whichFalse")
    protected Object doesInheritWT(REnvironment x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "whichFalse")
    protected Object doesInherit(VirtualFrame frame, RFunction x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(frame, x, what);
    }

    @Specialization(guards = "!whichFalse")
    protected Object doesInheritWT(RFunction x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "whichFalse")
    protected Object doesInherit(VirtualFrame frame, RSymbol x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(frame, x, what);
    }

    @Specialization(guards = "!whichFalse")
    protected Object doesInheritWT(RSymbol x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "whichFalse")
    protected byte doesInherit(VirtualFrame frame, RConnection x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(frame, x, what);
    }

    @Specialization(guards = "!whichFalse")
    protected Object doesInheritWT(RConnection x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassHierarchy(), what);
    }

    @Specialization(guards = "whichFalse")
    protected byte doesInherit(VirtualFrame frame, RAbstractContainer x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(frame, x, what);
    }

    @Specialization(guards = "!whichFalse")
    protected Object doesInherit(RAbstractVector x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassHierarchy(), what);
    }

    @TruffleBoundary
    // map operations lead to recursion resulting in compilation failure
    private static Object doDoesInherit(RStringVector classHr, RAbstractStringVector what) {
        Map<String, Integer> classToPos = InheritsNode.initClassToPos(classHr);
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

    }
}
