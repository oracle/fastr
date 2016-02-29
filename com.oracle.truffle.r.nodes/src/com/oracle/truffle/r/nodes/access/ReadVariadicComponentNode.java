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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * An {@link RNode} that handles accesses to components of the variadic argument (..1, ..2, etc.).
 */
public class ReadVariadicComponentNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxLookup, VisibilityController {

    @Child private ReadVariableNode lookup = ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any);
    @Child private PromiseHelperNode promiseHelper;

    private final int index;

    private final BranchProfile errorBranch = BranchProfile.create();
    private final BranchProfile promiseBranch = BranchProfile.create();

    public ReadVariadicComponentNode(SourceSection src, int index) {
        super(src);
        this.index = index;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        controlVisibility();

        Object args = lookup.execute(frame);
        if (args == null) {
            errorBranch.enter();
            throw RError.error(this, RError.Message.NO_DOT_DOT, index + 1);
        }
        RArgsValuesAndNames argsValuesAndNames = (RArgsValuesAndNames) args;
        if (argsValuesAndNames.isEmpty()) {
            errorBranch.enter();
            throw RError.error(this, RError.Message.NO_LIST_FOR_CDR);
        }

        if (argsValuesAndNames.getLength() <= index) {
            errorBranch.enter();
            throw RError.error(this, RError.Message.DOT_DOT_SHORT, index + 1);
        }
        Object ret = argsValuesAndNames.getArgument(index);
        if (ret instanceof RPromise) {
            promiseBranch.enter();
            // This might be the case, as lookup only checks for "..." to be a promise and forces it
            // eventually, NOT (all) of its content
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            ret = promiseHelper.evaluate(frame, (RPromise) ret);
        }
        return ret == null ? RMissing.instance : ret;
    }

    public String getPrintForm() {
        return ".." + Integer.toString(index + 1);
    }

    @Override
    public void deparseImpl(State state) {
        state.startNodeDeparse(this);
        state.append(getPrintForm());
        state.endNodeDeparse(this);
    }

    public RSyntaxNode substituteImpl(REnvironment env) {
        throw RInternalError.unimplemented();
    }

    public void serializeImpl(com.oracle.truffle.r.runtime.RSerialize.State state) {
        state.setCarAsSymbol(getPrintForm());
    }

    public String getIdentifier() {
        return getPrintForm();
    }

    public boolean isFunctionLookup() {
        return false;
    }
}
