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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin(name = "prod", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"...", "na.rm"})
public abstract class Prod extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"..."};

    @Override
    public String[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    @Child private Prod prodRecursive;

    public abstract Object executeObject(VirtualFrame frame, Object x);

    @Child private BinaryArithmetic prod = BinaryArithmetic.MULTIPLY.create();

    @Specialization
    protected Object prod(VirtualFrame frame, RArgsValuesAndNames args) {
        if (prodRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            prodRecursive = insert(ProdFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
        }
        // TODO: evantually handle multiple vectors properly
        return executeObject(frame, args.getValues()[0]);
    }

    @Specialization
    protected double prod(RAbstractDoubleVector x) {
        controlVisibility();
        double product = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            product = prod.op(product, x.getDataAt(k));
        }
        return product;
    }

    @Specialization
    protected double prod(RAbstractIntVector x) {
        controlVisibility();
        double product = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            product = prod.op(product, x.getDataAt(k));
        }
        return product;
    }

    @Specialization
    protected double prod(RAbstractLogicalVector x) {
        controlVisibility();
        double product = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            product = prod.op(product, x.getDataAt(k));
        }
        return product;
    }

    @Specialization
    protected RComplex prod(RAbstractComplexVector x) {
        controlVisibility();
        RComplex product = x.getDataAt(0);
        for (int k = 1; k < x.getLength(); ++k) {
            RComplex a = x.getDataAt(k);
            product = prod.op(product.getRealPart(), product.getImaginaryPart(), a.getRealPart(), a.getImaginaryPart());
        }
        return product;
    }

}
