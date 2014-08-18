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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.debug.Debug.Scope;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Run a FastR function, and dump its AST to IGV before and after running. If no function is passed,
 * this builtin does not do anything.
 */
@RBuiltin(name = "fastr.rundump", parameterNames = {"func"}, kind = PRIMITIVE)
public abstract class FastRRunDump extends RInvisibleBuiltinNode {

    // TODO Make this more versatile by allowing actual function calls with arguments to be
    // observed. This requires ... to work properly.

    @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

    private final GraphPrintVisitor graphPrinter = new GraphPrintVisitor();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RNull.instance)};
    }

    @Specialization
    public Object runDump(RNull function) {
        return function;
    }

    @Specialization
    public Object runDump(VirtualFrame frame, RFunction function) {
        controlVisibility();
        Object r = RNull.instance;
        graphPrinter.beginGroup(RRuntime.toString(function));
        try (Scope s = Debug.scope("FastR")) {
            graphPrinter.beginGraph("before").visit(function.getTarget().getRootNode());
            r = call.call(frame, function.getTarget(), RArguments.create(function));
            graphPrinter.beginGraph("after").visit(function.getTarget().getRootNode());
        } catch (Throwable t) {
            Debug.handle(t);
        } finally {
            graphPrinter.printToNetwork(true);
        }
        return r;
    }

}
