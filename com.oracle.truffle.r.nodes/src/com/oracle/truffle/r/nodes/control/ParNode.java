/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A {@link ParNode} represents value in parentheses "(...)" in source code.
 *
 * TODO: determine whether this node needs to implement {@link VisibilityController}
 */
public final class ParNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall {

    @Child private RNode value;

    public ParNode(SourceSection src, RNode value) {
        super(src);
        this.value = value;
    }

    public RNode getValue() {
        return value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return value.execute(frame);
    }

    @TruffleBoundary
    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        state.append('(');
        value.deparse(state);
        state.append(')');
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsLangType();
        state.setCarAsSymbol("(");
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(value);
        state.linkPairList(2);
    }

    @TruffleBoundary
    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return new ParNode(RSyntaxNode.EAGER_DEPARSE, value.substitute(env).asRNode());
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(getSourceSection(), "(", true);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{value.asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(1);
    }
}
