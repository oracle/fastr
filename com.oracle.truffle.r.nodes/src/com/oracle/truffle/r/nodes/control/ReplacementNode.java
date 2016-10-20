/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.RemoveAndAnswerNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Holds the sequence of nodes created for R's replacement assignment. Allows custom deparse and
 * debug handling.
 *
 */
public final class ReplacementNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {

    /**
     * This is just the left hand side of the assignment and only used when looking at the original
     * structure of this replacement.
     */
    private final RSyntaxNode syntaxLhs;
    private final String operator;

    /**
     * The original right hand side in the source can be found by {@code storeRhs.getRhs()}.
     */
    @Child private WriteVariableNode storeRhs;
    @Child private WriteVariableNode storeValue;
    @Children private final RNode[] updates;
    @Child private RemoveAndAnswerNode removeTemp;
    @Child private RemoveAndAnswerNode removeRhs;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    public ReplacementNode(SourceSection src, String operator, RSyntaxNode syntaxLhs, RSyntaxNode rhs, String rhsSymbol, RNode v, String tmpSymbol, List<RNode> updates) {
        super(src);
        assert "<-".equals(operator) || "<<-".equals(operator) || "=".equals(operator);
        this.operator = operator;
        this.syntaxLhs = syntaxLhs;
        this.storeRhs = WriteVariableNode.createAnonymous(rhsSymbol, rhs.asRNode(), WriteVariableNode.Mode.INVISIBLE);
        this.storeValue = WriteVariableNode.createAnonymous(tmpSymbol, v, WriteVariableNode.Mode.INVISIBLE);
        this.updates = updates.toArray(new RNode[updates.size()]);
        // remove var and rhs, returning rhs' value
        this.removeTemp = RemoveAndAnswerNode.create(tmpSymbol);
        this.removeRhs = RemoveAndAnswerNode.create(rhsSymbol);
    }

    /**
     * Support for syntax tree visitor.
     */
    public RSyntaxNode getLhs() {
        return syntaxLhs;
    }

    /**
     * Support for syntax tree visitor.
     */
    public RSyntaxNode getRhs() {
        return storeRhs.getRhs().asRSyntaxNode();
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        storeRhs.execute(frame);
        storeValue.execute(frame);
        for (RNode update : updates) {
            update.execute(frame);
        }
        removeTemp.execute(frame);
        try {
            return removeRhs.execute(frame);
        } finally {
            visibility.execute(frame, false);
        }
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(null, operator, true);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{syntaxLhs, storeRhs.getRhs().asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(2);
    }

    /**
     * Used by the parser for assignments that miss a left hand side. This node will raise an error
     * once executed.
     */
    public static final class LHSError extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {

        private final String operator;
        private final RSyntaxElement lhs;
        private final RSyntaxElement rhs;
        private final boolean nullError;

        public LHSError(SourceSection sourceSection, String operator, RSyntaxElement lhs, RSyntaxElement rhs, boolean nullError) {
            super(sourceSection);
            this.operator = operator;
            this.lhs = lhs;
            this.rhs = rhs;
            this.nullError = nullError;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            if (nullError) {
                throw RError.error(this, RError.Message.INVALID_NULL_LHS);
            } else if (lhs instanceof RSyntaxConstant) {
                throw RError.error(this, RError.Message.INVALID_LHS, "do_set");
            } else {
                throw RError.error(this, RError.Message.NON_LANG_ASSIGNMENT_TARGET);
            }
        }

        @Override
        public RSyntaxElement getSyntaxLHS() {
            return RSyntaxLookup.createDummyLookup(null, operator, true);
        }

        @Override
        public RSyntaxElement[] getSyntaxArguments() {
            return new RSyntaxElement[]{lhs, rhs};
        }

        @Override
        public ArgumentsSignature getSyntaxSignature() {
            return ArgumentsSignature.empty(2);
        }
    }
}
