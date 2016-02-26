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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Denotes a function body, which consists of a {@link SaveArgumentsNode} and a
 * {@link FunctionStatementsNode}.
 *
 * Children are typed as {@link RNode} to avoid a custom instrumentation wrapper node.
 *
 * The {@link SourceSection} is that of the {@link FunctionStatementsNode} as the
 * {@link SaveArgumentsNode} is not part of the syntax.
 */
public class FunctionBodyNode extends BodyNode implements RSyntaxNode {

    @CompilationFinal private SourceSection sourceSectionR;
    @Child private RNode saveArgs;

    public FunctionBodyNode(SaveArgumentsNode saveArgs, FunctionStatementsNode statements) {
        this(saveArgs, (RNode) statements);
    }

    private FunctionBodyNode(RNode saveArgs, RNode statements) {
        super(statements);
        this.saveArgs = saveArgs;
        // Same source section as statements but different tag
        this.sourceSectionR = statements.getSourceSection().withTags(RSyntaxTags.ENTER_FUNCTION);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        saveArgs.execute(frame);
        return statements.execute(frame);
    }

    public FunctionStatementsNode getStatements() {
        return (FunctionStatementsNode) statements.unwrap(); // statements may be wrapped
    }

    public FunctionDefinitionNode getFunctionDefinitionNode() {
        return (FunctionDefinitionNode) unwrapParent();
    }

    @Override
    public boolean isRInstrumentable() {
        return true;
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        RNode statementsSub = statements.substitute(env).asRNode();
        return new FunctionBodyNode(saveArgs, statementsSub);
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        // Don't deparse the argument saving nodes
        state.startNodeDeparse(this);
        statements.deparse(state);
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        // Don't serialize the argument saving nodes
        statements.serialize(state);
    }

    @Override
    public void allNamesImpl(RAllNames.State state) {
        statements.allNames(state);
    }

    public int getRlengthImpl() {
        boolean hasBrace = getFunctionDefinitionNode().hasBraces();
        FunctionStatementsNode fsNode = getStatements();
        int result = hasBrace ? 1 : 0;
        for (RNode s : fsNode.getSequence()) {
            if (s.unwrap() instanceof RSyntaxNode) {
                result++;
            }
        }
        return result;
    }

    @Override
    public Object getRelementImpl(int index) {
        boolean hasBrace = getFunctionDefinitionNode().hasBraces();
        FunctionStatementsNode fsNode = getStatements();
        int sIndex = hasBrace ? index - 1 : index;
        if (hasBrace && index == 0) {
            return RDataFactory.createSymbol("{");
        } else {
            return RASTUtils.createLanguageElement(fsNode.getSequence()[sIndex].unwrap());
        }
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        if (other instanceof FunctionBodyNode) {
            return getStatements().getRequalsImpl(((FunctionBodyNode) other).getStatements());
        } else {
            return false;
        }
    }

    public void setSourceSection(SourceSection sourceSection) {
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

    @Override
    public void unsetSourceSection() {
        sourceSectionR = null;
    }

}
