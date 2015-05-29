/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@TypeSystemReference(RTypes.class)
public abstract class RExternalBuiltinNode extends Node {

    public abstract Object call(RArgsValuesAndNames args);

    // TODO: these should be in the build nodes
    @Child private CastLogicalNode castLogical;
    @Child private CastIntegerNode castInt;
    @Child private CastDoubleNode castDouble;
    @Child private CastComplexNode castComplex;
    @Child private CastToVectorNode castVector;

    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    protected final BranchProfile errorProfile = BranchProfile.create();

    protected byte castLogical(RAbstractVector operand) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeGen.create(null, false, false, false));
        }
        return ((RAbstractLogicalVector) castLogical.executeCast(operand)).getDataAt(0);
    }

    protected int castInt(RAbstractVector operand) {
        if (castInt == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInt = insert(CastIntegerNodeGen.create(null, false, false, false));
        }
        return ((RAbstractIntVector) castInt.executeCast(operand)).getDataAt(0);
    }

    protected RAbstractDoubleVector castDouble(RAbstractVector operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(null, false, false, false));
        }
        return (RAbstractDoubleVector) castDouble.executeCast(operand);
    }

    protected RComplexVector castComplexVector(Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeGen.create(null, true, true, false));
        }
        return (RComplexVector) castComplex.executeCast(operand);
    }

    protected RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
        }
        return (RAbstractVector) castVector.executeObject(value);
    }

    protected static String isString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        } else if (arg instanceof RAbstractStringVector) {
            if (((RAbstractStringVector) arg).getLength() == 0) {
                return null;
            } else {
                return ((RAbstractStringVector) arg).getDataAt(0);
            }
        } else {
            return null;
        }
    }

    private static void checkLength(RArgsValuesAndNames args, int expectedLength) {
        if (args.getLength() != expectedLength) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("mismatching number of arguments to foreign function");
        }
    }

    public abstract static class Arg1 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 1);
            return execute(args.getArgument(0));
        }
    }

    public abstract static class Arg2 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 2);
            return execute(args.getArgument(0), args.getArgument(1));
        }
    }

    public abstract static class Arg3 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 3);
            return execute(args.getArgument(0), args.getArgument(1), args.getArgument(2));
        }
    }

    public abstract static class Arg4 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 4);
            return execute(args.getArgument(0), args.getArgument(1), args.getArgument(2), args.getArgument(3));
        }
    }

    public abstract static class Arg5 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 5);
            return execute(args.getArgument(0), args.getArgument(1), args.getArgument(2), args.getArgument(3), args.getArgument(4));
        }
    }

    public abstract static class Arg6 extends RExternalBuiltinNode {
        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 6);
            return execute(args.getArgument(0), args.getArgument(1), args.getArgument(2), args.getArgument(3), args.getArgument(4), args.getArgument(5));
        }
    }

    public abstract static class Arg7 extends RExternalBuiltinNode {

        public abstract Object execute(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

        @Override
        public final Object call(RArgsValuesAndNames args) {
            checkLength(args, 7);
            return execute(args.getArgument(0), args.getArgument(1), args.getArgument(2), args.getArgument(3), args.getArgument(4), args.getArgument(5), args.getArgument(6));
        }
    }
}
