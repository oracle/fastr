/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

public final class IfNode extends RNode implements RSyntaxNode, VisibilityController {

    @Child private ConvertBooleanNode condition;
    @Child private RNode thenPart;
    @Child private RNode elsePart;

    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    private IfNode(RSyntaxNode condition, RSyntaxNode thenPart, RSyntaxNode elsePart) {
        this.condition = ConvertBooleanNode.create(condition);
        this.thenPart = thenPart.asRNode();
        this.elsePart = elsePart == null ? null : elsePart.asRNode();
    }

    public static IfNode create(SourceSection src, RSyntaxNode condition, RSyntaxNode thenPart, RSyntaxNode elsePart) {
        IfNode i = new IfNode(condition, thenPart, elsePart == null ? null : elsePart);
        i.assignSourceSection(src);
        return i;
    }

    /**
     * Result visibility of an {@code if} expression is not only a property of the {@code if}
     * builtin; it also depends on whether there is an else branch or not, and on the condition. For
     * instance, the expression {@code if (FALSE) 23} will evaluate to {@code NULL}, but the result
     * will not be printed in the shell. Conversely, {@code NULL} will be printed for
     * {@code if (FALSE) 23 else NULL} because the else branch is given.
     */

    @Override
    public Object execute(VirtualFrame frame) {
        byte cond = condition.executeByte(frame);
        forceVisibility(elsePart != null || cond == RRuntime.LOGICAL_TRUE);

        if (cond == RRuntime.LOGICAL_NA) {
            // NA is the only remaining option
            CompilerDirectives.transferToInterpreter();
            throw RError.error(this, RError.Message.NA_UNEXP);
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
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
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
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin("if");
        state.openPairList(SEXPTYPE.LISTSXP);
        // condition
        state.serializeNodeSetCar(condition);
        // then, with brace
        state.openPairList(SEXPTYPE.LISTSXP);
        state.openBrace();
        state.serializeNodeSetCdr(thenPart, SEXPTYPE.LISTSXP);
        state.closeBrace();
        if (elsePart != null) {
            state.openPairList(SEXPTYPE.LISTSXP);
            state.openBrace();
            state.serializeNodeSetCdr(elsePart, SEXPTYPE.LISTSXP);
            state.closeBrace();
        }
        state.linkPairList(elsePart == null ? 2 : 3);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return create(null, condition.substitute(env), thenPart.substitute(env), elsePart == null ? null : elsePart.substitute(env));
    }

    @Override
    public int getRlengthImpl() {
        return 3 + (elsePart != null ? 1 : 0);
    }

    @Override
    public Object getRelementImpl(int index) {
        switch (index) {
            case 0:
                return RDataFactory.createSymbol("if");
            case 1:
                return RASTUtils.createLanguageElement(condition.getOperand());
            case 2:
                return RASTUtils.createLanguageElement(thenPart);
            case 3:
                return RASTUtils.createLanguageElement(elsePart);
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        if (other instanceof IfNode) {
            IfNode otherNode = (IfNode) other;
            if (condition.getRSyntaxNode().getRequalsImpl(otherNode.condition.getRSyntaxNode())) {
                if (thenPart.asRSyntaxNode().getRequalsImpl(otherNode.thenPart.asRSyntaxNode())) {
                    if (elsePart == null && otherNode.elsePart == null) {
                        return true;
                    } else if (elsePart != null && otherNode.elsePart != null) {
                        return elsePart.asRSyntaxNode().getRequalsImpl(otherNode.elsePart.asRSyntaxNode());
                    }
                }
            }
        }
        return false;
    }

}
