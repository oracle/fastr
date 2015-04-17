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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;

public final class WhileNode extends AbstractLoopNode {

    @Child private LoopNode loop;

    /**
     * Also used for {@code repeat}, with a {@code TRUE} condition.
     */
    private final boolean isRepeat;

    private WhileNode(RNode condition, RNode body, boolean isRepeat) {
        this.loop = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(ConvertBooleanNode.create(condition), body));
        this.isRepeat = isRepeat;
    }

    public static WhileNode create(RNode condition, RNode body, boolean isRepeat) {
        return new WhileNode(condition, body, isRepeat);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loop.executeLoop(frame);
        return RNull.instance;
    }

    public ConvertBooleanNode getCondition() {
        return getRepeatingNode().getCondition();
    }

    public RNode getBody() {
        return getRepeatingNode().getBody();
    }

    private WhileRepeatingNode getRepeatingNode() {
        return (WhileRepeatingNode) loop.getRepeatingNode();
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(RDeparse.State state) {
        if (isRepeat) {
            state.append("repeat ");
        } else {
            state.append("while (");
            getCondition().deparse(state);
            state.append(") ");
        }
        state.writeOpenCurlyNLIncIndent();
        getBody().deparse(state);
        state.decIndentWriteCloseCurly();
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setAsBuiltin(isRepeat ? "repeat" : "while");
        if (!isRepeat) {
            state.openPairList(SEXPTYPE.LISTSXP);
            // condition
            state.serializeNodeSetCar(getCondition());
        }
        state.openPairList(SEXPTYPE.LISTSXP);
        state.openBrace();
        state.serializeNodeSetCdr(getBody(), SEXPTYPE.LISTSXP);
        state.closeBrace();
        state.linkPairList(isRepeat ? 1 : 2);
        state.setCdr(state.closePairList());
    }

    @Override
    public RNode substitute(REnvironment env) {
        return create(getCondition().substitute(env), getBody().substitute(env), isRepeat);
    }

    private static final class WhileRepeatingNode extends Node implements RepeatingNode {

        @Child private ConvertBooleanNode condition;
        @Child private RNode body;

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        public WhileRepeatingNode(ConvertBooleanNode condition, RNode body) {
            this.condition = condition;
            this.body = body;
        }

        public RNode getBody() {
            return body;
        }

        public ConvertBooleanNode getCondition() {
            return condition;
        }

        public boolean executeRepeating(VirtualFrame frame) {
            try {
                if (conditionProfile.profile(condition.executeByte(frame) == RRuntime.LOGICAL_TRUE)) {
                    body.execute(frame);
                    return true;
                } else {
                    return false;
                }
            } catch (BreakException e) {
                breakBlock.enter();
                return false;
            } catch (NextException e) {
                nextBlock.enter();
                return true;
            }
        }
    }
}
