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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("sapply")
public abstract class SApply extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"X", "FUN", "USE.NAMES"};

    @Child private CallNode callNode;
    @Child private WriteVariableNode writeNode;
    @Child private CastStringNode castString;

    private final Object temporaryVariableSymbol = new Object();

    private static final int TARGET_ARRAY_UNINITIALIZED = 0;
    private static final int TARGET_ARRAY_INT = 1;
    private static final int TARGET_ARRAY_DOUBLE = 2;
    private static final int TARGET_ARRAY_GENERIC = -1;

    @CompilationFinal private int targetArray = TARGET_ARRAY_UNINITIALIZED;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    private final NACheck check = new NACheck();

    @Specialization
    public Object sapply(VirtualFrame frame, RAbstractVector vector, RFunction fun, byte namesEnabled) {
        int len = vector.getLength();
        RAbstractVector resultVector;
        try {
            if (targetArray == TARGET_ARRAY_INT) {
                resultVector = sapplyInt(frame, vector, fun, len);
            } else if (targetArray == TARGET_ARRAY_DOUBLE) {
                resultVector = sapplyDouble(frame, vector, fun, len);
            } else {
                resultVector = sapplyGeneric(frame, new Object[len], 0, vector, fun);
            }
            applyNames(frame, vector, resultVector, namesEnabled);
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                LoopNode.reportLoopCount(this, len);
            }
        }
        return resultVector;
    }

    private RAbstractVector sapplyDouble(VirtualFrame frame, RAbstractVector vector, RFunction fun, int len) {
        double[] result = new double[len];
        for (int i = 0; i < len; i++) {
            write(frame, vector.getDataAtAsObject(i));
            try {
                result[i] = callDouble(frame, fun);
            } catch (UnexpectedResultException e) {
                targetArray = TARGET_ARRAY_GENERIC;
                Object[] recoveryResult = new Object[len];
                for (int j = 0; j < i; j++) {
                    recoveryResult[j] = result[j];
                }
                recoveryResult[i] = e.getResult();
                return sapplyGeneric(frame, recoveryResult, i + 1, vector, fun);
            }
            check.check(result[i]);
        }
        return RDataFactory.createDoubleVector(result, check.neverSeenNA());
    }

    private RAbstractVector sapplyInt(VirtualFrame frame, RAbstractVector vector, RFunction fun, int len) {
        int[] result = new int[len];
        for (int i = 0; i < len; i++) {
            write(frame, vector.getDataAtAsObject(i));
            try {
                result[i] = callInt(frame, fun);
            } catch (UnexpectedResultException e) {
                targetArray = TARGET_ARRAY_GENERIC;
                Object[] recoveryResult = new Object[len];
                for (int j = 0; j < i; j++) {
                    recoveryResult[j] = result[j];
                }
                recoveryResult[i] = e.getResult();
                return sapplyGeneric(frame, recoveryResult, i + 1, vector, fun);
            }
            check.check(result[i]);
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    private RAbstractVector sapplyGeneric(VirtualFrame frame, Object[] array, int start, RAbstractVector vector, RFunction fun) {
        for (int i = start; i < array.length; i++) {
            write(frame, vector.getDataAtAsObject(i));
            Object value = call(frame, fun);
            array[i] = value;
            if (targetArray != TARGET_ARRAY_GENERIC) {
                profileTargetArray(value);
            }
        }
        if (targetArray == TARGET_ARRAY_INT) {
            int[] result = new int[array.length];
            for (int i = 0; i < array.length; ++i) {
                result[i] = (int) array[i];
            }
            return RDataFactory.createIntVector(result, check.neverSeenNA());
        } else if (targetArray == TARGET_ARRAY_DOUBLE) {
            double[] result = new double[array.length];
            for (int i = 0; i < array.length; ++i) {
                result[i] = (double) array[i];
            }
            return RDataFactory.createDoubleVector(result, check.neverSeenNA());
        } else {
            // generic
            return RDataFactory.createList(array);
        }
    }

    private void profileTargetArray(Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        if (targetArray == TARGET_ARRAY_UNINITIALIZED) {
            if (value instanceof Integer) {
                targetArray = TARGET_ARRAY_INT;
            } else if (value instanceof Double) {
                targetArray = TARGET_ARRAY_DOUBLE;
            } else {
                targetArray = TARGET_ARRAY_GENERIC;
            }
        } else if (targetArray == TARGET_ARRAY_INT) {
            if (!(value instanceof Integer)) {
                targetArray = TARGET_ARRAY_GENERIC;
            }
        } else if (targetArray == TARGET_ARRAY_DOUBLE) {
            if (!(value instanceof Double)) {
                targetArray = TARGET_ARRAY_GENERIC;
            }
        }
    }

    private void applyNames(VirtualFrame frame, RAbstractVector sourceVector, RAbstractVector resultVector, byte enabled) {
        if (resultVector instanceof RVector) {
            // TODO hack to make this work
            // this should be a names(resultVector) <- names(vector) call instead.
            RVector resultVectorCast = (RVector) resultVector;
            resultVectorCast.setNames(sourceVector instanceof RStringVector ? sourceVector : sourceVector.getNames());
            if (RRuntime.LOGICAL_TRUE == enabled) {
                if (resultVectorCast.getNames() == null) {
                    resultVectorCast.setNames(castString(frame, sourceVector));
                }
            }
        }
    }

    public static class CreateArray extends RNode {
        @Override
        public Object execute(VirtualFrame frame) {
            throw new AssertionError();
        }
    }

    private Object call(VirtualFrame frame, RFunction function) {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreter();
            callNode = adoptChild(CallNode.createCall(null, CallArgumentsNode.createUnnamed(ReadVariableNode.create(temporaryVariableSymbol, false, false))));
        }
        return callNode.execute(frame, function);
    }

    private int callInt(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreter();
            callNode = adoptChild(CallNode.createCall(null, CallArgumentsNode.createUnnamed(ReadVariableNode.create(temporaryVariableSymbol, false, false))));
        }
        return RTypesGen.RTYPES.expectInteger(callNode.execute(frame, function));
    }

    private double callDouble(VirtualFrame frame, RFunction function) throws UnexpectedResultException {
        if (callNode == null) {
            CompilerDirectives.transferToInterpreter();
            callNode = adoptChild(CallNode.createCall(null, CallArgumentsNode.createUnnamed(ReadVariableNode.create(temporaryVariableSymbol, false, false))));
        }
        return RTypesGen.RTYPES.expectDouble(callNode.execute(frame, function));
    }

    private void write(VirtualFrame frame, Object value) {
        if (writeNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeNode = adoptChild(WriteVariableNode.create(temporaryVariableSymbol, null, false, false));
        }
        writeNode.execute(frame, value);
    }

    private RStringVector castString(VirtualFrame frame, RAbstractVector value) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreter();
            castString = adoptChild(CastStringNodeFactory.create(null, false, true, false));
        }
        return (RStringVector) castString.executeString(frame, value);
    }

}
