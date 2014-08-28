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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "tabulate", kind = RBuiltinKind.INTERNAL, parameterNames = {"bin", "nbins"})
public abstract class Tabulate extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    @Specialization(guards = {"isValidNBin"})
    public RIntVector tabulate(RAbstractIntVector bin, int nBins) {
        controlVisibility();
        int[] ans = new int[nBins];
        for (int i = 0; i < bin.getLength(); i++) {
            int currentEl = bin.getDataAt(i);
            if (!RRuntime.isNA(currentEl) && currentEl > 0 && currentEl <= nBins) {
                ans[currentEl - 1]++;
            }
        }
        return RDataFactory.createIntVector(ans, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    @Specialization
    public RIntVector tabulate(Object bin, int nBins) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INPUT);
    }

    protected boolean isValidNBin(@SuppressWarnings("unused") RAbstractIntVector bin, int nBins) {
        if (RRuntime.isNA(nBins) || nBins < 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "nbin");
        }
        return true;
    }

}
