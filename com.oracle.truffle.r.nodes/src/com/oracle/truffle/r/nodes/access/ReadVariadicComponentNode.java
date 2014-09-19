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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.expressions.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;

/**
 * An {@link RNode} that handles accesses to components of the variadic argument (..1, ..2, etc.).
 */
public class ReadVariadicComponentNode extends RNode {

    @Child private ReadVariableNode lookup = ReadVariableNode.create("...", RRuntime.TYPE_ANY, false, true);
    @Child private ExpressionExecutorNode exprExecNode = ExpressionExecutorNode.create();

    private final int index;

    private final PromiseProfile promiseProfile = new PromiseProfile();

    public ReadVariadicComponentNode(int index) {
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object args = null;
        try {
            args = lookup.execute(frame);
        } catch (RError e) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_DOT_DOT, index + 1);
        }
        RArgsValuesAndNames argsValuesAndNames = null;
        if (args == RMissing.instance) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_LIST_FOR_CDR);
        } else {
            argsValuesAndNames = (RArgsValuesAndNames) args;
        }
        if (argsValuesAndNames.length() <= index) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.DOT_DOT_SHORT, index + 1);
        }
        Object ret = argsValuesAndNames.getValues()[index];
        if (ret instanceof RPromise) {
            // This might be the case, as lookup only checks for "..." to be a promise and forces it
            // eventually, NOT (all) of its content
            ret = PromiseHelper.evaluate(frame, exprExecNode, (RPromise) ret, promiseProfile);
        }
        return ret == null ? RMissing.instance : ret;
    }
}
