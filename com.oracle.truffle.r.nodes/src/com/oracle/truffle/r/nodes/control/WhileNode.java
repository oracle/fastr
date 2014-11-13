/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.REnvironment;

@SuppressWarnings("unused")
public final class WhileNode extends LoopNode {

    @Child private ConvertBooleanNode condition;
    @Child private RNode body;

    private WhileNode(RNode condition, RNode body) {
        this.condition = ConvertBooleanNode.create(condition);
        this.body = body;
    }

    public static WhileNode create(RNode condition, RNode body) {
        return new WhileNode(condition, body);
    }

    public static WhileNode create(SourceSection src, RNode condition, RNode body) {
        WhileNode wn = create(condition, body);
        wn.assignSourceSection(src);
        return wn;
    }

    public ConvertBooleanNode getCondition() {
        return condition;
    }

    public RNode getBody() {
        return body;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        state.append("while (");
        condition.deparse(state);
        state.append(") ");
        state.writeOpenCurlyNLIncIndent();
        body.deparse(state);
        state.decIndentWriteCloseCurly();
    }

    @Override
    public RNode substitute(REnvironment env) {
        return create(condition.substitute(env), body.substitute(env));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        int count = 0;
        try {
            while (condition.executeByte(frame) == RRuntime.LOGICAL_TRUE) {
                try {
                    body.execute(frame);
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }
                } catch (NextException e) {
                }
            }
        } catch (BreakException e) {
        } finally {
            if (CompilerDirectives.inInterpreter()) {
                reportLoopCount(count);
            }
        }
        return RNull.instance;
    }
}
