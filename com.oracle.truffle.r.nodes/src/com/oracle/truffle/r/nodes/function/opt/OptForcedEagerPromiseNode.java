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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.function.ArgumentStatePush;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A optimizing {@link PromiseNode}: It evaluates a constant directly.
 */
public final class OptForcedEagerPromiseNode extends PromiseNode {

    @Child private RNode expr;
    @Child private PromiseHelperNode promiseHelper;

    private final BranchProfile nonPromiseProfile = BranchProfile.create();
    private final RPromiseFactory factory;
    private final int wrapIndex;

    private static final Assumption alwaysValidAssumption = Truffle.getRuntime().createAssumption("always valid (forced eager promise)");

    public OptForcedEagerPromiseNode(RPromiseFactory factory, int wrapIndex) {
        super(null);
        this.factory = factory;
        this.wrapIndex = wrapIndex;
        this.expr = (RNode) factory.getExpr();
    }

    /**
     * Creates a new {@link RPromise} every time.
     */
    @Override
    public Object execute(final VirtualFrame frame) {
        Object value;
        // need to unwrap as re-wrapping happens when the value is retrieved (otherwise ref count
        // update happens twice)
        if (wrapIndex != ArgumentStatePush.INVALID_INDEX && expr instanceof WrapArgumentNode) {
            value = ((WrapArgumentNode) expr).getOperand().execute(frame);
        } else {
            value = expr.execute(frame);
        }
        if (value instanceof RPromise) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            value = promiseHelper.evaluate(frame, (RPromise) value);
        } else {
            nonPromiseProfile.enter();
        }
        RCaller call = RArguments.getCall(frame);
        while (call.isPromise()) {
            call = call.getParent();
        }
        return factory.createEagerSuppliedPromise(value, alwaysValidAssumption, call, null, wrapIndex);
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return getPromiseExpr();
    }

    @Override
    public RSyntaxNode getPromiseExpr() {
        return expr.asRSyntaxNode();
    }
}
