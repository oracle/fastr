/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.SUMMARY_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUMMARY;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@ImportStatic(RType.class)
@RBuiltin(name = "prod", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE_SUMMARY)
public abstract class Prod extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(Prod.class);
        casts.arg("na.rm").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(Predef.toBoolean());
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Child private BinaryArithmetic prod = BinaryArithmetic.MULTIPLY.createOperation();

    @ExplodeLoop
    protected static boolean supports(RArgsValuesAndNames args, VectorAccess[] argAccess) {
        if (args.getLength() != argAccess.length) {
            return false;
        }
        for (int i = 0; i < argAccess.length; i++) {
            if (!argAccess[i].supports(args.getArgument(i))) {
                return false;
            }
        }
        return true;
    }

    protected static VectorAccess[] createAccess(RArgsValuesAndNames args, RType topmostType) {
        VectorAccess[] result = new VectorAccess[args.getLength()];
        for (int i = 0; i < result.length; i++) {
            VectorAccess access = VectorAccess.create(args.getArgument(i));
            if (access == null) {
                return null;
            }
            RType type = access.getType();
            if (type != RType.Null && type != RType.Logical && type != RType.Integer && type != RType.Double && type != topmostType) {
                return null;
            }
            result[i] = access;
        }
        return result;
    }

    @Specialization(guards = {"argAccess != null", "supports(args, argAccess)", "naRm == cachedNaRm"})
    @ExplodeLoop
    protected double prodDoubleCached(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean naRm,
                    @Cached("naRm") boolean cachedNaRm,
                    @Cached("createAccess(args, Double)") VectorAccess[] argAccess) {
        double value = 1;
        for (int i = 0; i < argAccess.length; i++) {
            VectorAccess access = argAccess[i];
            double element = prodDouble(args.getArgument(i), access, cachedNaRm);
            if (!cachedNaRm && access.na.check(element)) {
                return element;
            }
            value *= element;
        }
        return value;
    }

    @Specialization(guards = {"argAccess != null", "supports(args, argAccess)", "naRm == cachedNaRm"})
    @ExplodeLoop
    protected RComplex prodComplexCached(RArgsValuesAndNames args, @SuppressWarnings("unused") boolean naRm,
                    @Cached("naRm") boolean cachedNaRm,
                    @Cached("createAccess(args, Complex)") VectorAccess[] argAccess) {
        RComplex value = RComplex.valueOf(1, 0);
        for (int i = 0; i < argAccess.length; i++) {
            VectorAccess access = argAccess[i];
            RComplex element = prodComplex(args.getArgument(i), access, cachedNaRm);
            if (!cachedNaRm && access.na.check(element)) {
                return element;
            }
            value = prod.op(value.getRealPart(), value.getImaginaryPart(), element.getRealPart(), element.getImaginaryPart());
        }
        return value;
    }

    @Specialization(replaces = {"prodDoubleCached", "prodComplexCached"})
    protected Object prodGeneric(RArgsValuesAndNames args, boolean naRm) {
        int length = args.getLength();
        double value = 1;
        int i = 0;
        for (; i < length; i++) {
            Object arg = args.getArgument(i);
            VectorAccess access = VectorAccess.createSlowPath(arg);
            if (access == null) {
                break;
            }
            RType type = access.getType();
            if (type != RType.Null && type != RType.Logical && type != RType.Integer && type != RType.Double) {
                break;
            }
            double element = prodDouble(arg, access, naRm);
            if (!naRm && access.na.check(element)) {
                return element;
            }
            value *= element;
        }
        if (i == length) {
            return value;
        }
        RComplex complexValue = RComplex.valueOf(1, 0);
        for (; i < length; i++) {
            Object arg = args.getArgument(i);
            VectorAccess access = VectorAccess.createSlowPath(arg);
            if (access == null) {
                break;
            }
            RType type = access.getType();
            if (!type.isNumeric() && type != RType.Null) {
                break;
            }
            RComplex element = prodComplex(arg, access, naRm);
            if (!naRm && access.na.check(element)) {
                return element;
            }
            complexValue = prod.op(complexValue.getRealPart(), complexValue.getImaginaryPart(), element.getRealPart(), element.getImaginaryPart());
        }
        if (i == length) {
            return complexValue;
        }
        throw error(RError.Message.INVALID_TYPE_ARGUMENT, Predef.typeName().apply(args.getArgument(i)));
    }

    protected static double prodDouble(Object v, VectorAccess access, boolean naRm) {
        try (SequentialIterator iter = access.access(v)) {
            double value = 1;
            while (access.next(iter)) {
                double element = access.getDouble(iter);
                if (access.na.check(element)) {
                    if (!naRm) {
                        return RRuntime.DOUBLE_NA;
                    }
                } else {
                    value *= element;
                }
            }
            return value;
        }
    }

    protected RComplex prodComplex(Object v, VectorAccess access, boolean naRm) {
        try (SequentialIterator iter = access.access(v)) {
            RComplex value = RComplex.valueOf(1, 0);
            while (access.next(iter)) {
                RComplex element = access.getComplex(iter);
                if (access.na.check(element)) {
                    if (!naRm) {
                        return element;
                    }
                } else {
                    value = prod.op(value.getRealPart(), value.getImaginaryPart(), element.getRealPart(), element.getImaginaryPart());
                }
            }
            return value;
        }
    }

    @Fallback
    protected Object prod(Object v, @SuppressWarnings("unused") Object naRm) {
        throw error(RError.Message.INVALID_TYPE_ARGUMENT, Predef.typeName().apply(v));
    }
}
