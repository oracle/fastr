/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.binary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;

public final class CombineFoldOperationNode extends FoldOperationNode {

    @Child private CombineBinaryComplexNode combineComplex;
    @Child private CombineBinaryDoubleNode combineDouble;
    @Child private CombineBinaryIntegerNode combineInteger;
    @Child private CombineBinaryLogicalNode combineLogical;
    @Child private CombineBinaryStringNode combineString;
    @Child private CombineBinaryRawNode combineRaw;
    @Child private CombineBinaryListNode combineList;

    @Override
    public Object executeComplex(VirtualFrame frame, Object left, Object right) {
        if (combineComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineComplex = insert(CombineBinaryComplexNodeGen.create(null, null));
        }
        return combineComplex.executeCombine(frame, left, right);
    }

    @Override
    public Object executeDouble(VirtualFrame frame, Object left, Object right) {
        if (combineDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineDouble = insert(CombineBinaryDoubleNodeGen.create(null, null));
        }
        return combineDouble.executeCombine(frame, left, right);
    }

    @Override
    public Object executeInteger(VirtualFrame frame, Object left, Object right) {
        if (combineInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineInteger = insert(CombineBinaryIntegerNodeGen.create(null, null));
        }
        return combineInteger.executeCombine(frame, left, right);
    }

    @Override
    public Object executeString(VirtualFrame frame, Object left, Object right) {
        if (combineString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineString = insert(CombineBinaryStringNodeGen.create(null, null));
        }
        return combineString.executeCombine(frame, left, right);
    }

    @Override
    public Object executeLogical(VirtualFrame frame, Object left, Object right) {
        if (combineLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineLogical = insert(CombineBinaryLogicalNodeGen.create(null, null));
        }
        return combineLogical.executeCombine(frame, left, right);
    }

    @Override
    public Object executeRaw(VirtualFrame frame, Object left, Object right) {
        if (combineRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineRaw = insert(CombineBinaryRawNodeGen.create(null, null));
        }
        return combineRaw.executeCombine(frame, left, right);
    }

    @Override
    public Object executeList(VirtualFrame frame, Object left, Object right) {
        if (combineList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineList = insert(CombineBinaryListNodeGen.create(null, null));
        }
        return combineList.executeCombine(frame, left, right);
    }
}
