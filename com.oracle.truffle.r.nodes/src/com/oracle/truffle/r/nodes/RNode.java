/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.ProbeNode.*;
import com.oracle.truffle.r.nodes.instrument.NeedsWrapper;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.nodes.instrument.wrappers.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@TypeSystemReference(RTypes.class)
@NeedsWrapper
public abstract class RNode extends Node implements RInstrumentableNode {

    @CompilationFinal public static final RNode[] EMTPY_RNODE_ARRAY = new RNode[0];
    @CompilationFinal protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public abstract Object execute(VirtualFrame frame);

    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectInteger(execute(frame));
    }

    public RRaw executeRRaw(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRRaw(execute(frame));
    }

    public RAbstractVector executeRAbstractVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractVector(execute(frame));
    }

    public RComplex executeRComplex(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRComplex(execute(frame));
    }

    public RIntSequence executeRIntSequence(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRIntSequence(execute(frame));
    }

    public RDoubleSequence executeRDoubleSequence(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRDoubleSequence(execute(frame));
    }

    public RIntVector executeRIntVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRIntVector(execute(frame));
    }

    public RDoubleVector executeRDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRDoubleVector(execute(frame));
    }

    public RRawVector executeRRawVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRRawVector(execute(frame));
    }

    public RComplexVector executeRComplexVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRComplexVector(execute(frame));
    }

    public RStringVector executeRStringVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRStringVector(execute(frame));
    }

    public RList executeRList(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRList(execute(frame));
    }

    public RLogicalVector executeRLogicalVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRLogicalVector(execute(frame));
    }

    public RAbstractDoubleVector executeRAbstractDoubleVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractDoubleVector(executeRAbstractVector(frame));
    }

    public RAbstractIntVector executeRAbstractIntVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractIntVector(executeRAbstractVector(frame));
    }

    public RAbstractComplexVector executeRAbstractComplexVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractComplexVector(executeRAbstractVector(frame));
    }

    public RAbstractLogicalVector executeRAbstractLogicalVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractLogicalVector(executeRAbstractVector(frame));
    }

    public RAbstractRawVector executeRAbstractRawVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractRawVector(executeRAbstractVector(frame));
    }

    public RAbstractStringVector executeRAbstractStringVector(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractStringVector(executeRAbstractVector(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectDouble(execute(frame));
    }

    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectByte(execute(frame));
    }

    public Object[] executeArray(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectObjectArray(execute(frame));
    }

    public RFunction executeFunction(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRFunction(execute(frame));
    }

    public RNull executeNull(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRNull(execute(frame));
    }

    public RMissing executeMissing(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRMissing(execute(frame));
    }

    public String executeString(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectString(execute(frame));
    }

    public REnvironment executeREnvironment(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectREnvironment(execute(frame));
    }

    public RConnection executeRConnection(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRConnection(execute(frame));
    }

    public RExpression executeRExpression(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRExpression(execute(frame));
    }

    public RDataFrame executeRDataFrame(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRDataFrame(execute(frame));
    }

    public RFactor executeRFactor(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRFactor(execute(frame));
    }

    public RSymbol executeRSymbol(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRSymbol(execute(frame));
    }

    public RLanguage executeRLanguage(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRLanguage(execute(frame));
    }

    public RPromise executeRPromise(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRPromise(execute(frame));
    }

    public RAbstractContainer executeRAbstractContainer(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRAbstractContainer(execute(frame));
    }

    public RPairList executeRPairList(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRPairList(execute(frame));
    }

    public RFormula executeFormula(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRFormula(execute(frame));
    }

    public RArgsValuesAndNames executeRArgsValuesAndNames(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRArgsValuesAndNames(execute(frame));
    }

    public RType executeType(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectRType(execute(frame));
    }

    public static boolean areSameLength(RAbstractContainer a, RAbstractContainer b) {
        return a.getLength() == b.getLength();
    }

    @Override
    public WrapperNode createWrapperNode() {
        return new RNodeWrapper(this);
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    private static final long WORK_SCALE_FACTOR = 100;

    /**
     * Nodes that can do a significant amount of work in one execution (like arithmetic on vectors)
     * can use this method to report the work to the Truffle system, similar to loop counts.
     *
     * @param amount an approximation of the number of operations
     */
    protected void reportWork(long amount) {
        reportWork(this, amount);
    }

    public static void reportWork(Node base, long amount) {
        if (CompilerDirectives.inInterpreter()) {
            if (amount >= WORK_SCALE_FACTOR) {
                int scaledAmount = (int) (amount / WORK_SCALE_FACTOR);
                if (amount > 0) {
                    RootNode root = base.getRootNode();
                    if (root != null && root.getCallTarget() instanceof LoopCountReceiver) {
                        ((LoopCountReceiver) root.getCallTarget()).reportLoopCount(scaledAmount);
                    }
                }
            }
        }
    }

    protected static boolean isRAbstractContainer(Object value) {
        return value instanceof RAbstractContainer;
    }

    protected static boolean isRAbstractVector(Object value) {
        return value instanceof RAbstractVector;
    }

    /*
     * No isXyz functions for abstract vector classes (RAbstractIntVector, etc.), because they do
     * not honor implicit casts and can thus lead to unexpected behavior.
     */

    protected static boolean isRList(Object value) {
        return value instanceof RList;
    }

    protected static boolean isRDataFrame(Object value) {
        return value instanceof RDataFrame;
    }

    protected static boolean isRFactor(Object value) {
        return value instanceof RFactor;
    }

    protected static boolean isRPromise(Object value) {
        return value instanceof RPromise;
    }

    protected static boolean isRLanguage(Object value) {
        return value instanceof RLanguage;
    }

    protected static boolean isRExpression(Object value) {
        return value instanceof RExpression;
    }

    protected static boolean isRFunction(Object value) {
        return value instanceof RFunction;
    }

    protected static boolean isREnvironment(Object value) {
        return value instanceof REnvironment;
    }

    protected static boolean isRConnection(Object value) {
        return value instanceof RConnection;
    }

    protected static boolean isRPairList(Object value) {
        return value instanceof RPairList;
    }

    protected static boolean isRSymbol(Object value) {
        return value instanceof RSymbol;
    }

    protected static boolean isRArgsValuesAndNames(Object value) {
        return value instanceof RArgsValuesAndNames;
    }

    protected static boolean isRMissing(Object value) {
        return value == RMissing.instance;
    }

    protected static boolean isRNull(Object value) {
        return value == RNull.instance;
    }

    /**
     * Returns the {@link RSyntaxNode} associated with this node. In the case that this does not
     * implement {@link RSyntaxNode} the assumption is that this is a child of one that can be
     * retrieved by following the parent chain.
     */
    protected RSyntaxNode getRSyntaxNode() {
        if (this instanceof RSyntaxNode) {
            return (RSyntaxNode) this;
        } else {
            Node current = this;
            while (current != null) {
                if (current instanceof RSyntaxNode) {
                    return (RSyntaxNode) current;
                }
                current = current.getParent();
                if (current instanceof WrapperNode) {
                    current = current.getParent();
                }
            }
            throw RInternalError.shouldNotReachHere("getRSynaxNode");
        }
    }
}
