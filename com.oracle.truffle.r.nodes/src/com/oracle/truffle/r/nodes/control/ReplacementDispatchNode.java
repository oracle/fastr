/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableSyntaxNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Represents a replacement consisting of execution of the RHS, call to the actual replacement
 * sequence and removal of RHS returning the RHS value to the caller. The actual replacement is
 * created lazily. Moreover, we use 'special' fast-path version of replacement where possible with
 * fallback to generic implementation.
 */
public final class ReplacementDispatchNode extends OperatorNode {

    // these are only @Child to make instrumentation work
    @Child private RNode lhs;
    @Child private RNode rhs;

    private final boolean isSuper;
    private final int tempNamesStartIndex;

    public ReplacementDispatchNode(SourceSection src, RSyntaxLookup operator, RSyntaxNode lhs, RSyntaxNode rhs, boolean isSuper, int tempNamesStartIndex) {
        super(src, operator);
        assert lhs != null && rhs != null;
        this.lhs = lhs.asRNode();
        this.rhs = rhs.asRNode();
        this.isSuper = isSuper;
        this.tempNamesStartIndex = tempNamesStartIndex;
    }

    @Override
    public void voidExecute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        create(true).voidExecute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return create(false).execute(frame);

    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return create(false).visibleExecute(frame);
    }

    public RNode create(boolean isVoid) {
        RNode replacement;
        RSyntaxNode lhsSyntax = lhs.asRSyntaxNode();
        if (lhsSyntax instanceof RSyntaxCall) {
            replacement = createReplacementNode(isVoid);
        } else {
            replacement = createWriteVariableNode(lhsSyntax);
        }
        return replace(replacement);
    }

    private RNode createWriteVariableNode(RSyntaxNode lhsSyntax) {
        String name;
        if (lhsSyntax instanceof RSyntaxLookup) {
            name = ((RSyntaxLookup) lhsSyntax).getIdentifier();
        } else if (lhsSyntax instanceof RSyntaxConstant) {
            RSyntaxConstant c = (RSyntaxConstant) lhsSyntax;
            if (c.getValue() instanceof String) {
                name = (String) c.getValue();
            } else {
                // "this" needs to be initialized for error reporting to work
                throw RError.error(this, RError.Message.INVALID_LHS, "do_set");
            }
        } else {
            throw RInternalError.unimplemented("unexpected lhs type in replacement: " + lhsSyntax.getClass());
        }
        if (name.isEmpty()) {
            throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
        }
        return new WriteVariableSyntaxNode(getLazySourceSection(), operator, lhsSyntax, name, rhs, isSuper);
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(2);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{lhs.asRSyntaxNode(), rhs.asRSyntaxNode()};
    }

    private ReplacementNode createReplacementNode(boolean isVoid) {
        CompilerAsserts.neverPartOfCompilation();

        /*
         * Collect all the function calls in this replacement. For "a(b(x)) <- z", this would be
         * "a(...)" and "b(...)".
         */
        List<RSyntaxCall> calls = new ArrayList<>();
        RSyntaxElement current = lhs.asRSyntaxNode();
        while (!(current instanceof RSyntaxLookup)) {
            if (!(current instanceof RSyntaxCall)) {
                if (current instanceof RSyntaxConstant && ((RSyntaxConstant) current).getValue() == RNull.instance) {
                    throw RError.error(this, RError.Message.INVALID_NULL_LHS);
                } else {
                    throw RError.error(this, RError.Message.NON_LANG_ASSIGNMENT_TARGET);
                }
            }
            RSyntaxCall call = (RSyntaxCall) current;
            calls.add(call);

            RSyntaxElement syntaxLHS = call.getSyntaxLHS();
            if (call.getSyntaxArguments().length == 0 || !(syntaxLHS instanceof RSyntaxLookup || isNamespaceLookupCall(syntaxLHS))) {
                throw RError.error(this, RError.Message.INVALID_NULL_LHS);
            }
            current = call.getSyntaxArguments()[0];
        }
        RSyntaxLookup variable = (RSyntaxLookup) current;
        ReadVariableNode varRead = createReplacementForVariableUsing(variable, isSuper);
        return ReplacementNode.create(getLazySourceSection(), operator, varRead, lhs.asRSyntaxNode(), rhs, calls, variable.getIdentifier(), isSuper, tempNamesStartIndex, isVoid);
    }

    private static ReadVariableNode createReplacementForVariableUsing(RSyntaxLookup var, boolean isSuper) {
        if (isSuper) {
            return ReadVariableNode.createSuperLookup(var.getLazySourceSection(), var.getIdentifier());
        } else {
            return ReadVariableNode.create(var.getLazySourceSection(), var.getIdentifier(), true);
        }
    }

    /*
     * Determines if syntax call is of the form foo::bar
     */
    private static boolean isNamespaceLookupCall(RSyntaxElement e) {
        if (e instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) e;
            // check for syntax nodes as this will be required to recreate a call during
            // replacement form construction in createFunctionUpdate
            if (call.getSyntaxLHS() instanceof RSyntaxLookup) {
                if (((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals("::")) {
                    RSyntaxElement[] args = call.getSyntaxArguments();
                    if (args.length == 2 && args[0] instanceof RSyntaxLookup && args[1] instanceof RSyntaxLookup) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * Encapsulates check for the specific structure of replacements, to display the replacement
     * instead of the "internal" form (with *tmp*, etc.) of the update call.
     */
    public static RLanguage getRLanguage(RLanguage language) {
        RSyntaxNode sn = (RSyntaxNode) language.getRep();
        Node parent = RASTUtils.unwrapParent(sn.asNode());
        if (parent instanceof WriteVariableNode) {
            WriteVariableNode wvn = (WriteVariableNode) parent;
            return ReplacementNode.getLanguage(wvn);
        }
        return null;
    }
}
