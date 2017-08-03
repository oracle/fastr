/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * {@link WrapArgumentBaseNode} is a super class of wrappers handling function arguments.
 */
public abstract class WrapArgumentBaseNode extends RNode {

    @Child protected RNode operand;

    private final ConditionProfile isShareable = ConditionProfile.createBinaryProfile();

    protected WrapArgumentBaseNode(RNode operand) {
        this.operand = operand;
    }

    public final RNode getOperand() {
        return operand;
    }

    @Override
    public final void voidExecute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Object result = operand.execute(frame);
        return execute(frame, result);
    }

    @Override
    public final Object visibleExecute(VirtualFrame frame) {
        Object result = operand.visibleExecute(frame);
        return execute(frame, result);
    }

    public Object execute(VirtualFrame frame, Object result) {
        RSharingAttributeStorage.verify(result);
        if (isShareable.profile(result instanceof RSharingAttributeStorage)) {
            return handleShareable(frame, (RSharingAttributeStorage) result);
        } else {
            return result;
        }
    }

    protected abstract Object handleShareable(VirtualFrame frame, RSharingAttributeStorage shareable);

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return operand == null ? null : operand.asRSyntaxNode();
    }

    @Override
    public SourceSection getSourceSection() {
        return operand == null ? null : operand.getSourceSection();
    }
}
