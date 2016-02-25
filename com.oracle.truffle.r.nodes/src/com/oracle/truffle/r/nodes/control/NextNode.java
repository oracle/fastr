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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class NextNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall, VisibilityController {

    public NextNode(SourceSection src) {
        super(src);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        forceVisibility(false);
        throw NextException.instance;
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        state.append("next");
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin("next");
    }

    public RSyntaxNode substituteImpl(REnvironment env) {
        return this;
    }

    public int getRlengthImpl() {
        return 1;
    }

    @Override
    public Object getRelementImpl(int index) {
        return RDataFactory.createSymbol("next");
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        throw RInternalError.unimplemented();
    }

    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(getSourceSection(), "next", true);
    }

    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[0];
    }

    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(0);
    }
}
