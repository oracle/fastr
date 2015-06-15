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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
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

    protected abstract Object execute(Object x, Object what, Object which);

    @Child private InheritsNode inheritsNode;
    @Child private Inherits recursiveInherits;

    private InheritsNode initInheritsNode() {
        if (inheritsNode == null) {
            inheritsNode = insert(com.oracle.truffle.r.nodes.unary.InheritsNodeGen.create());
        }
        return inheritsNode;
    }

    protected static boolean isTrue(byte value) {
        return RRuntime.fromLogical(value);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doesInherit(RNull x, RAbstractStringVector what, byte which) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = "!isTrue(which)")
    protected Object doesInherit(REnvironment x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(x, what);
    }

    @Specialization(guards = "isTrue(which)")
    protected Object doesInheritWT(REnvironment x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "!isTrue(which)")
    protected Object doesInherit(RFunction x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(x, what);
    }

    @Specialization(guards = "isTrue(which)")
    protected Object doesInheritWT(RFunction x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "!isTrue(which)")
    protected Object doesInherit(RSymbol x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(x, what);
    }

    @Specialization(guards = "isTrue(which)")
    protected Object doesInheritWT(RSymbol x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassAttr(attrProfiles), what);
    }

    @Specialization(guards = "!isTrue(which)")
    protected byte doesInherit(RConnection x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(x, what);
    }

    @Specialization(guards = "isTrue(which)")
    protected Object doesInheritWT(RConnection x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassHierarchy(), what);
    }

    @Specialization(guards = "!isTrue(which)")
    protected byte doesInherit(RAbstractContainer x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return initInheritsNode().execute(x, what);
    }

    @Specialization(guards = "isTrue(which)")
    protected Object doesInherit(RAbstractVector x, RAbstractStringVector what, @SuppressWarnings("unused") byte which) {
        return doDoesInherit(x.getClassHierarchy(), what);
    }

    @Specialization(guards = "!isTrue(which)")
    protected Object doesInherit(RArgsValuesAndNames x, RAbstractStringVector what, byte which) {
        assert x.getLength() == 1;
        if (recursiveInherits == null) {
            recursiveInherits = insert(com.oracle.truffle.r.nodes.builtin.base.InheritsNodeGen.create(null, null, null));
        }
        return recursiveInherits.execute(x.getArgument(0), what, which);
    }

    @TruffleBoundary
    // map operations lead to recursion resulting in compilation failure
    private static Object doDoesInherit(RStringVector classHr, RAbstractStringVector what) {
        Map<String, Integer> classToPos = InheritsNode.initClassToPos(classHr);
        int[] result = new int[what.getLength()];
        for (int i = 0; i < what.getLength(); i++) {
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
