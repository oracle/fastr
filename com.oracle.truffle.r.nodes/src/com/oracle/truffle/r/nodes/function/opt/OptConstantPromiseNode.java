/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.OptType;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;

/**
 * A optimizing {@link PromiseNode}: It evaluates a constant directly.
 */
public final class OptConstantPromiseNode extends RNode {

    private final PromiseType type;
    private final ConstantNode constantExpr;
    private final Object constantValue;

    @Child private WrapArgumentNode wrapNode;

    public OptConstantPromiseNode(PromiseType type, ConstantNode constantExpr, WrapArgumentNode wrapNode) {
        this.type = type;
        this.constantExpr = constantExpr;
        this.constantValue = constantExpr.getValue();
        this.wrapNode = wrapNode;
    }

    /**
     * Creates a new {@link RPromise} every time.
     */
    @Override
    public Object execute(VirtualFrame frame) {
        if (wrapNode != null) {
            wrapNode.execute(frame, constantValue);
        }
        return RDataFactory.createPromise(type, OptType.DEFAULT, constantExpr, constantValue);
    }
}
