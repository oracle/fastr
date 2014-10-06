/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseProfile;

/**
 * Temporary substitutions that just evaluate the expression for package loading and assume no
 * errors or finally statements.
 */
public class TryFunctions {
    @RBuiltin(name = "try", kind = RBuiltinKind.SUBSTITUTE, parameterNames = {"expr", "silent"}, nonEvalArgs = {0})
    public abstract static class Try extends RBuiltinNode {

        @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);

        private final PromiseProfile promiseProfile = new PromiseProfile();

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
        }

        @Specialization
        protected Object doTry(VirtualFrame frame, RPromise expr, @SuppressWarnings("unused") byte silent) {
            controlVisibility();
            return PromiseHelper.evaluate(frame, promiseExpressionCache, expr, promiseProfile);
        }
    }

    // Ignoring finally completely
    @RBuiltin(name = "tryCatch", kind = RBuiltinKind.SUBSTITUTE, parameterNames = {"expr", "..."}, nonEvalArgs = {-1})
    public abstract static class TryCatch extends RBuiltinNode {

        @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);

        private final PromiseProfile promiseProfile = new PromiseProfile();

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(EMPTY_OBJECT_ARRAY)};
        }

        @Specialization
        protected Object doTryCatch(VirtualFrame frame, RPromise expr, RPromise arg) {
            return doTryCatch(frame, expr, new Object[]{arg});
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doTryCatch(VirtualFrame frame, RPromise expr, Object[] args) {
            controlVisibility();
            return PromiseHelper.evaluate(frame, promiseExpressionCache, expr, promiseProfile);
        }
    }
}
