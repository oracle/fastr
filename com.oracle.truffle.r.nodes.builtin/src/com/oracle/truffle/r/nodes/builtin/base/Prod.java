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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "prod", kind = PRIMITIVE, parameterNames = {"...", "na.rm"}, dispatch = SUMMARY_GROUP_GENERIC, behavior = PURE_SUMMARY)
public abstract class Prod extends RBuiltinNode.Arg2 {

    // TODO: handle multiple arguments, handle na.rm

    static {
        Casts.noCasts(Prod.class);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_FALSE};
    }

    @Child private Prod prodRecursive;

    public abstract Object executeObject(Object x);

    @Child private BinaryArithmetic prod = BinaryArithmetic.MULTIPLY.createOperation();

    @Specialization
    protected Object prod(RArgsValuesAndNames args) {
        int argsLen = args.getLength();
        if (argsLen == 0) {
            return 1d;
        }
        if (prodRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            prodRecursive = insert(ProdNodeGen.create());
        }
        Object ret = 1d;
        if (argsLen > 0) {
            double prodReal;
            double prodImg;
            boolean complex;
            if (ret instanceof RComplex) {
                RComplex c = (RComplex) ret;
                prodReal = c.getRealPart();
                prodImg = c.getImaginaryPart();
                complex = true;
            } else {
                prodReal = (Double) ret;
                prodImg = 0d;
                complex = false;
            }
            for (int i = 0; i < argsLen; i++) {
                Object aProd = prodRecursive.executeObject(args.getArgument(i));
                double aProdReal;
                double aProdImg;
                if (aProd instanceof RComplex) {
                    RComplex c = (RComplex) aProd;
                    if (RRuntime.isNA(c)) {
                        return c;
                    }
                    aProdReal = c.getRealPart();
                    aProdImg = c.getImaginaryPart();
                    complex = true;
                } else {
                    aProdReal = (Double) aProd;
                    aProdImg = 0d;
                    if (RRuntime.isNA(aProdReal)) {
                        return aProd;
                    }
                }
                if (complex) {
                    RComplex c = prod.op(prodReal, prodImg, aProdReal, aProdImg);
                    prodReal = c.getRealPart();
                    prodImg = c.getImaginaryPart();
                } else {
                    prodReal = prod.op(prodReal, aProdReal);
                }
            }
            ret = complex ? RComplex.valueOf(prodReal, prodImg) : prodReal;
        }
        return ret;
    }

    private final ValueProfile intVecProfile = ValueProfile.createClassProfile();
    private final NACheck naCheck = NACheck.create();

    @Specialization
    protected double prod(RAbstractDoubleVector x) {
        RAbstractDoubleVector profiledVec = intVecProfile.profile(x);
        double product = 1;
        naCheck.enable(x);
        for (int k = 0; k < profiledVec.getLength(); k++) {
            double value = profiledVec.getDataAt(k);
            if (naCheck.check(value)) {
                return RRuntime.DOUBLE_NA;
            }
            product = prod.op(product, value);
        }
        return product;
    }

    @Specialization
    protected double prod(RAbstractIntVector x) {
        RAbstractIntVector profiledVec = intVecProfile.profile(x);
        double product = 1;
        naCheck.enable(x);
        for (int k = 0; k < profiledVec.getLength(); k++) {
            int data = profiledVec.getDataAt(k);
            if (naCheck.check(data)) {
                return RRuntime.DOUBLE_NA;
            }
            product = prod.op(product, data);
        }
        return product;
    }

    @Specialization
    protected double prod(RAbstractLogicalVector x) {
        RAbstractLogicalVector profiledVec = intVecProfile.profile(x);
        double product = 1;
        naCheck.enable(x);
        for (int k = 0; k < profiledVec.getLength(); k++) {
            byte value = profiledVec.getDataAt(k);
            if (naCheck.check(value)) {
                return RRuntime.DOUBLE_NA;
            }
            product = prod.op(product, value);
        }
        return product;
    }

    @Specialization
    protected RComplex prod(RAbstractComplexVector x) {
        RAbstractComplexVector profiledVec = intVecProfile.profile(x);
        RComplex product = RDataFactory.createComplexRealOne();
        naCheck.enable(x);
        for (int k = 0; k < profiledVec.getLength(); k++) {
            RComplex a = profiledVec.getDataAt(k);
            if (naCheck.check(a)) {
                return a;
            }
            product = prod.op(product.getRealPart(), product.getImaginaryPart(), a.getRealPart(), a.getImaginaryPart());
        }
        return product;
    }

    @Specialization
    protected double prod(@SuppressWarnings("unused") RNull n) {
        return 1d;
    }

    @Fallback
    protected Object prod(Object o) {
        throw error(RError.Message.INVALID_TYPE_ARGUMENT, Predef.typeName().apply(o));
    }
}
