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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@TypeSystemReference(RTypes.class)
public abstract class RNode extends Node {

    public static final RNode[] EMTPY_RNODE_ARRAY = new RNode[0];
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public abstract Object execute(VirtualFrame frame);

    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectInteger(execute(frame));
    }

    public RRaw executeRRaw(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRRaw(execute(frame));
    }

    public RAbstractVector executeRAbstractVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractVector(execute(frame));
    }

    public RComplex executeRComplex(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRComplex(execute(frame));
    }

    public RIntSequence executeRIntSequence(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRIntSequence(execute(frame));
    }

    public RDoubleSequence executeRDoubleSequence(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRDoubleSequence(execute(frame));
    }

    public RIntVector executeRIntVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRIntVector(execute(frame));
    }

    public RDoubleVector executeRDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRDoubleVector(execute(frame));
    }

    public RRawVector executeRRawVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRRawVector(execute(frame));
    }

    public RComplexVector executeRComplexVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRComplexVector(execute(frame));
    }

    public RStringVector executeRStringVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRStringVector(execute(frame));
    }

    public RList executeRList(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRList(execute(frame));
    }

    public RLogicalVector executeRLogicalVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRLogicalVector(execute(frame));
    }

    public RAbstractDoubleVector executeRAbstractDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractDoubleVector(executeRAbstractVector(frame));
    }

    public RAbstractIntVector executeRAbstractIntVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractIntVector(executeRAbstractVector(frame));
    }

    public RAbstractComplexVector executeRAbstractComplexVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractComplexVector(executeRAbstractVector(frame));
    }

    public RAbstractLogicalVector executeRAbstractLogicalVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractLogicalVector(executeRAbstractVector(frame));
    }

    public RAbstractRawVector executeRAbstractRawVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractRawVector(executeRAbstractVector(frame));
    }

    public RAbstractStringVector executeRAbstractStringVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRAbstractStringVector(executeRAbstractVector(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectDouble(execute(frame));
    }

    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectByte(execute(frame));
    }

    public Object[] executeArray(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectObjectArray(execute(frame));
    }

    public RFunction executeFunction(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRFunction(execute(frame));
    }

    public RNull executeNull(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRNull(execute(frame));
    }

    public RMissing executeMissing(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRMissing(execute(frame));
    }

    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectString(execute(frame));
    }

    public REnvironment executeREnvironment(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectREnvironment(execute(frame));
    }

    public RConnection executeRConnection(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRConnection(execute(frame));
    }

    public RInvisible executeRInvisible(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.RTYPES.expectRInvisible(execute(frame));
    }

    public static boolean areSameLength(RAbstractVector a, RAbstractVector b) {
        return a.getLength() == b.getLength();
    }

    @SlowPath
    public static final void warning(String warning) {
        RContext.getInstance().setEvalWarning(warning);
    }

}
