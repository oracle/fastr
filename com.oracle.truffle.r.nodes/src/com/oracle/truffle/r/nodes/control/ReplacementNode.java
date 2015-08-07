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

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Holds the sequence of nodes created for R's replacement assignment. Allows custom deparse and
 * debug handling.
 */
public final class ReplacementNode extends RNode implements RSyntaxNode {

    /**
     * This holds the AST for the "untransformed" AST, i.e. as it appears in the source. Currently
     * only used in {@code deparse} and {@code serialize}.
     */
    @CompilationFinal private RSyntaxNode syntaxAST;

    @Child private WriteVariableNode storeRhs;
    @Child private WriteVariableNode storeValue;
    @Child private RNode update;
    @Child private RemoveAndAnswerNode removeTemp;
    @Child private RemoveAndAnswerNode removeRhs;

    public ReplacementNode(SourceSection src, RNode rhs, RNode v, boolean copyRhs, RNode update, String tmpSymbol, String rhsSymbol) {
        this.storeRhs = WriteVariableNode.createAnonymous(rhsSymbol, rhs, copyRhs ? WriteVariableNode.Mode.COPY : WriteVariableNode.Mode.INVISIBLE);
        this.storeValue = WriteVariableNode.createAnonymous(tmpSymbol, v, WriteVariableNode.Mode.INVISIBLE);
        this.update = update;
        // remove var and rhs, returning rhs' value
        this.removeTemp = RemoveAndAnswerNode.create(tmpSymbol);
        this.removeRhs = RemoveAndAnswerNode.create(rhsSymbol);
        assignSourceSection(src);
    }

    public void setSyntaxAST(RSyntaxNode syntaxAST) {
        // No sharing between syntaxAST and child nodes
        if (syntaxAST instanceof WriteReplacementNode) {
            // already taken care of
            this.syntaxAST = syntaxAST;
        } else {
            this.syntaxAST = (RSyntaxNode) NodeUtil.cloneNode(syntaxAST.asNode());
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        storeRhs.execute(frame);
        storeValue.execute(frame);
        update.execute(frame);
        removeTemp.execute(frame);
        return removeRhs.execute(frame);
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        syntaxAST.deparseImpl(state);
        state.endNodeDeparse(this);
    }

    private static ReplacementNode current;

    @Override
    public void serializeImpl(RSerialize.State state) {
        if (this == current) {
            throw RInternalError.shouldNotReachHere("replacement recursion");
        }
        current = this;
        syntaxAST.serializeImpl(state);
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return syntaxAST.substituteImpl(env);
    }
}
