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

package com.oracle.truffle.r.nodes.unary;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Basic support for "inherits" that is used by the {@code inherits} builtin and others.
 */
public abstract class InheritsNode extends BinaryNode {
    public abstract byte execute(VirtualFrame frame, Object x, Object what);

    @SuppressWarnings("unused")
    @Specialization
    protected Object doesInherit(RNull x, RAbstractStringVector what) {
        return RRuntime.LOGICAL_FALSE;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doesInherit(REnvironment x, RAbstractStringVector what) {
        return RRuntime.LOGICAL_FALSE;
    }

    // map operations lead to recursion resulting in compilation failure
    @Specialization
    protected Object doesInherit(RAbstractVector x, RAbstractStringVector what) {
        Map<String, Integer> classToPos = initClassToPos(x);
        for (int i = 0; i < what.getLength(); ++i) {
            if (classToPos.get(what.getDataAt(i)) != null) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @SlowPath
    public static Map<String, Integer> initClassToPos(RAbstractVector x) {
        RStringVector klass = x.getClassHierarchy();

        // Create a mapping for elements to their respective positions
        // in the vector for faster lookup.
        Map<String, Integer> classToPos = new HashMap<>();
        for (int i = 0; i < klass.getLength(); ++i) {
            classToPos.put(klass.getDataAt(i), i);
        }
        return classToPos;
    }
}
