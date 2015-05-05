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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Holds the sequence of nodes created for R's replacement assignment. Allows custom deparse and
 * debug handling.
 */
public class ReplacementNode extends RNode implements RSyntaxNode {

    /**
     * This holds the AST for the "untransformed" AST, i.e. as it appears in the source. Currently
     * only used in {@code deparse} and {@code serialize}.
     */
    @CompilationFinal private RSyntaxNode syntaxAST;

    @Children protected final RNode[] sequence;

    public ReplacementNode(SourceSection src, RNode[] seq) {
        this.sequence = seq;
        assignSourceSection(src);
    }

    public void setSyntaxAST(RSyntaxNode syntaxAST) {
        this.syntaxAST = syntaxAST;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object lastResult = null;
        for (int i = 0; i < sequence.length; i++) {
            lastResult = sequence[i].execute(frame);
        }
        return lastResult;
    }

    @Override
    public void deparse(RDeparse.State state) {
        syntaxAST.deparse(state);
    }

    @Override
    public void serialize(RSerialize.State state) {
        syntaxAST.serialize(state);
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        return syntaxAST.substitute(env);
    }
}
