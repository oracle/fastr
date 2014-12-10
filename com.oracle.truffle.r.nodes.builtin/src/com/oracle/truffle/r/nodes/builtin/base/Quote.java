/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "quote", nonEvalArgs = {0}, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"expr"})
public abstract class Quote extends RBuiltinNode {
    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance)};
    }

    public abstract Object execute(VirtualFrame frame, RPromise expr);

    private final ConditionProfile rvn = ConditionProfile.createBinaryProfile();
    private final ConditionProfile cn = ConditionProfile.createBinaryProfile();

    @Specialization
    protected RLanguage doQuote(@SuppressWarnings("unused") RMissing arg) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
    }

    @Specialization
    protected Object doQuote(RPromise expr) {
        controlVisibility();
        // GnuR creates symbols for simple variables and actual values for constants
        RNode node = (RNode) expr.getRep();
        RNode unode = (RNode) RASTUtils.unwrap(node);
        SourceSection ss = node.getSourceSection();
        if (rvn.profile(unode instanceof ReadVariableNode)) {
            return RDataFactory.createSymbol(ss.toString());
        } else if (cn.profile(unode instanceof ConstantNode)) {
            ConstantNode cnode = (ConstantNode) unode;
            return cnode.getValue();
        } else {
            return RDataFactory.createLanguage(expr.getRep());
        }
    }
}
