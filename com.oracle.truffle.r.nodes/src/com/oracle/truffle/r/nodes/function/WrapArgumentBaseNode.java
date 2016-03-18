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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RDataFrame;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * {@link WrapArgumentBaseNode} is a super class of wrappers handling function arguments.
 */
public abstract class WrapArgumentBaseNode extends RNode {

    @Child protected RNode operand;

    private final BranchProfile everSeenVector;
    private final BranchProfile everSeenDataFrame;
    private final BranchProfile everSeenFactor;
    private final BranchProfile everSeenLanguage;
    private final BranchProfile everSeenFunction;
    private final BranchProfile everSeenS4Object;

    protected final BranchProfile shareable;
    private final BranchProfile nonShareable;

    protected WrapArgumentBaseNode(RNode operand, boolean initProfiles) {
        this.operand = operand;
        if (initProfiles) {
            everSeenVector = BranchProfile.create();
            everSeenDataFrame = BranchProfile.create();
            everSeenFactor = BranchProfile.create();
            everSeenLanguage = BranchProfile.create();
            everSeenFunction = BranchProfile.create();
            everSeenS4Object = BranchProfile.create();
            shareable = BranchProfile.create();
            nonShareable = BranchProfile.create();
        } else {
            everSeenVector = null;
            everSeenDataFrame = null;
            everSeenFactor = null;
            everSeenLanguage = null;
            everSeenFunction = null;
            everSeenS4Object = null;
            shareable = null;
            nonShareable = null;
        }
    }

    protected RShareable getShareable(Object result) {
        if (result instanceof RVector) {
            everSeenVector.enter();
            return (RVector) result;
        } else if (result instanceof RDataFrame) {
            everSeenDataFrame.enter();
            return ((RDataFrame) result).getVector();
        } else if (result instanceof RFactor) {
            everSeenFactor.enter();
            return ((RFactor) result).getVector();
        } else if (result instanceof RLanguage) {
            everSeenLanguage.enter();
            return (RLanguage) result;
        } else if (result instanceof RFunction) {
            everSeenFunction.enter();
            return (RFunction) result;
        } else if (result instanceof RS4Object) {
            everSeenS4Object.enter();
            return (RS4Object) result;
        } else {
            nonShareable.enter();
            return null;
        }
    }

    public RNode getOperand() {
        return operand;
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return getOperand().asRSyntaxNode();
    }
}
