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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;

public class IfNode extends RNode implements VisibilityController {

    @Child private ConvertBooleanNode condition;
    @Child private RNode thenPart;
    @Child private RNode elsePart;

    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    protected IfNode(RNode condition, RNode thenPart, RNode elsePart) {
        this.condition = ConvertBooleanNode.create(condition);
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    public static IfNode create(RNode condition, RNode thenPart, RNode elsePart) {
        return new IfNode(condition, thenPart, elsePart);
    }

    public static IfNode create(SourceSection src, RNode condition, RNode thenPart, RNode elsePart) {
        IfNode i = create(condition, thenPart, elsePart);
        i.assignSourceSection(src);
        return i;
    }

    /**
     * Result visibility of an {@code if} expression is not only a property of the {@code if}
     * builtin; it also depends on whether there is an else branch or not, and on the condition. For
     * instance, the expression {@code if (FALSE) 23} will evaluate to {@code NULL}, but the result
     * will not be printed in the shell. Conversely, {@code NULL} will be printed for
     * {@code if (FALSE) 23 else NULL} because the else branch is given.
     *
     * This means that we need to take care of visibility in this class, and do a double check of
     * the condition and the presence of an else branch below in {@link #execute}.
     */
    private boolean isVisible = true;

    @Override
    public boolean getVisibility() {
        return isVisible;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @CreateCast({"condition"})
    public ConvertBooleanNode conditionToBoolean(RNode node) {
        return ConvertBooleanNode.create(node);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        byte cond = condition.executeByte(frame);
        if (!RContext.isHeadless()) {
            isVisible = cond == RRuntime.LOGICAL_TRUE || elsePart != null;
        }
        controlVisibility();

        if (cond == RRuntime.LOGICAL_NA) {
            // NA is the only remaining option
            CompilerDirectives.transferToInterpreter();
            throw RError.error(getSourceSection(), RError.Message.NA_UNEXP);
        }

        if (conditionProfile.profile(cond == RRuntime.LOGICAL_TRUE)) {
            return thenPart.execute(frame);
        } else {
            assert cond == RRuntime.LOGICAL_FALSE : "logical value none of TRUE|FALSE|NA";

            if (elsePart != null) {
                return elsePart.execute(frame);
            } else {
                return RNull.instance;
            }
        }
    }

    public ConvertBooleanNode getCondition() {
        return condition;
    }

    public RNode getThenPart() {
        return thenPart;
    }

    public RNode getElsePart() {
        return elsePart;
    }

    @Override
    public void deparse(State state) {
        /*
         * We have a problem with { }, since they do not exist as AST nodes (functions), so we
         * insert them routinely, which means we can't match GnuR output in simple cases.
         */
        state.append("if (");
        condition.deparse(state);
        state.append(") ");
        state.writeOpenCurlyNLIncIndent();
        thenPart.deparse(state);
        state.decIndentWriteCloseCurly();
        if (elsePart != null) {
            state.append(" else ");
            state.writeOpenCurlyNLIncIndent();
            elsePart.deparse(state);
            state.decIndentWriteCloseCurly();
        }

    }
}
