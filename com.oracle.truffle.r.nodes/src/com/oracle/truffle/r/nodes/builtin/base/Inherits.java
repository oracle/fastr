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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "inherits")
public abstract class Inherits extends RBuiltinNode {

    @Child private GetClass getClass;

    private Object getClassAttr(VirtualFrame frame, RAbstractVector x) {
        if (getClass == null) {
            CompilerDirectives.transferToInterpreter();
            getClass = adoptChild(GetClassFactory.create(new RNode[1], getBuiltin()));
        }
        return getClass.execute(frame, x);
    }

    @Specialization
    public Object doesInherit(VirtualFrame frame, RAbstractVector x, RAbstractStringVector what, byte which) {
        boolean isWhich = which == RRuntime.LOGICAL_TRUE;
        Object attr = getClassAttr(frame, x);

        if (attr instanceof RStringVector) {
            RStringVector klass = (RStringVector) attr;
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
        } else {
            if (!isWhich) {
                for (int i = 0; i < what.getLength(); ++i) {
                    if (what.getDataAt(i).equals(attr)) {
                        return RRuntime.LOGICAL_TRUE;
                    }
                }
                return RRuntime.LOGICAL_FALSE;
            }
            int[] result = new int[what.getLength()];
            for (int i = 0; i < what.getLength(); ++i) {
                if (what.getDataAt(i).equals(attr)) {
                    result[i] = 1;
                }
            }
            return RDataFactory.createIntVector(result, true);
        }
    }

    @Specialization
    public Object doesInherit(VirtualFrame frame, RAbstractVector x, RAbstractStringVector what, RMissing which) {
        return doesInherit(frame, x, what, RRuntime.LOGICAL_FALSE);
    }

    @Specialization
    public Object doesInherit(VirtualFrame frame, RAbstractVector x, RAbstractStringVector what, Object which) {
        throw RError.getNotLengthOneLogicalVector(getSourceSection(), RRuntime.WHICH);
    }

    @Specialization
    public Object doesInherit(VirtualFrame frame, RAbstractVector x, Object what, Object which) {
        throw RError.getNotCharacterVector(getSourceSection(), RRuntime.WHAT);
    }
}
