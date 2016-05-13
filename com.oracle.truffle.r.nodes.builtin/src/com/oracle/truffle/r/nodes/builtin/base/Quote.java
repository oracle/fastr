/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.ReadVariadicComponentNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "quote", nonEvalArgs = 0, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"expr"})
public abstract class Quote extends RBuiltinNode {

    public abstract Object execute(VirtualFrame frame, RPromise expr);

    private final ConditionProfile rvn = ConditionProfile.createBinaryProfile();
    private final ConditionProfile rvcn = ConditionProfile.createBinaryProfile();
    private final ConditionProfile cn = ConditionProfile.createBinaryProfile();

    @Specialization
    protected RLanguage doQuote(@SuppressWarnings("unused") RMissing arg) {
        throw RError.error(this, RError.Message.ARGUMENTS_PASSED_0_1, getRBuiltin().name());
    }

    @Specialization
    protected Object doQuote(RPromise expr) {
        // GnuR creates symbols for simple variables and actual values for constants
        RNode node = (RNode) expr.getRep();
        RNode unode = (RNode) RASTUtils.unwrap(node);
        if (rvn.profile(unode instanceof ReadVariableNode)) {
            return RASTUtils.createRSymbol(unode);
        } else if (cn.profile(unode instanceof ConstantNode)) {
            ConstantNode cnode = (ConstantNode) unode;
            return cnode.getValue();
        } else if (rvcn.profile(unode instanceof ReadVariadicComponentNode)) {
            return RASTUtils.createRSymbol(unode);
        } else {
            return RDataFactory.createLanguage(unode);
        }
    }
}
