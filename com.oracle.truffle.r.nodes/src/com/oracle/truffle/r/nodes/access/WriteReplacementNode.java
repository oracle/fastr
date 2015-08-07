/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * A node that current only exists as the {@code syntaxAST} argument of a {@link ReplacementNode},
 * for assignments of the form {@code f(a) <- rhs}.
 *
 * There must be no sharing of nodes between the {@code syntaxAST} and the child nodes of the
 * ReplacementNode as this will cause runaway recursion due to the check for a node being a child of
 * a {@link ReplacementNode} in {@link RBaseNode#getRSyntaxNode}.
 */
public class WriteReplacementNode extends RNode implements RSyntaxNode {

    private final RCallNode replacementCall;
    private final RSyntaxNode rhs;

    public RNode getReplacementCall() {
        return replacementCall;
    }

    public WriteReplacementNode(RCallNode replacementCall, RSyntaxNode rhs) {
        this.replacementCall = NodeUtil.cloneNode(replacementCall);
        this.rhs = (RSyntaxNode) NodeUtil.cloneNode(rhs.asNode());
    }

    public RSyntaxNode getRhs() {
        return rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        getReplacementCall().deparse(state);
        state.append(" <- ");
        getRhs().deparseImpl(state);
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin("<-");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getReplacementCall());
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getRhs());
        state.linkPairList(2);
        state.setCdr(state.closePairList());
    }

    public RSyntaxNode substituteImpl(REnvironment env) {
        throw RInternalError.unimplemented();
    }

}
