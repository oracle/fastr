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

@RBuiltin(name = "which.max", kind = RBuiltinKind.INTERNAL)
public abstract class WhichMax extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[0] = CastDoubleNodeFactory.create(arguments[0], true, false, false);
        return arguments;
    }

    @Specialization
    public int which(RAbstractDoubleVector x) {
        controlVisibility();
        double max = x.getDataAt(0);
        int max_index = 0;
        for (int i = 0; i < x.getLength(); i++) {
            if (x.getDataAt(i) > max) {
                max = x.getDataAt(i);
                max_index = i;
            }
        }
        return max_index + 1;
    }

    @Specialization
    public int which(@SuppressWarnings("unused") Object x) {
        controlVisibility();
        throw RError.getNonNumericMath(this.getEncapsulatingSourceSection());
    }
}
