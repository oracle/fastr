/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class ConditionalMapNode extends CastNode {

    private final ArgumentFilter<Object, Object> argFilter;
    private final boolean resultForNull;
    private final boolean resultForMissing;
    private final boolean returns;

    @Child private CastNode trueBranch;
    @Child private CastNode falseBranch;

    protected ConditionalMapNode(ArgumentFilter<Object, Object> argFilter, CastNode trueBranch, CastNode falseBranch, boolean resultForNull, boolean resultForMissing, boolean returns) {
        this.argFilter = argFilter;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
        this.resultForNull = resultForNull;
        this.resultForMissing = resultForMissing;
        this.returns = returns;
    }

    public static ConditionalMapNode create(ArgumentFilter<Object, Object> argFilter, CastNode trueBranch, CastNode falseBranch, boolean resultForNull, boolean resultForMissing, boolean returns) {
        return ConditionalMapNodeGen.create(argFilter, trueBranch, falseBranch, resultForNull, resultForMissing, returns);
    }

    private Object executeConditional(boolean isTrue, Object x) {
        if (isTrue) {
            Object result = trueBranch == null ? x : trueBranch.execute(x);
            if (returns) {
                throw new PipelineReturnException(result);
            } else {
                return result;
            }
        } else {
            return falseBranch == null ? x : falseBranch.execute(x);
        }
    }

    @Specialization
    protected Object executeNull(RNull x) {
        return executeConditional(resultForNull, x);
    }

    @Specialization
    protected Object executeMissing(RMissing x) {
        return executeConditional(resultForMissing, x);
    }

    @Specialization(guards = {"!isRNull(x)", "!isRMissing(x)"})
    protected Object executeRest(Object x,
                    @Cached("createBinaryProfile()") ConditionProfile conditionProfile) {
        return executeConditional(conditionProfile.profile(argFilter.test(x)), x);
    }

    @SuppressWarnings("serial")
    public final class PipelineReturnException extends ControlFlowException {

        private final Object result;

        public PipelineReturnException(Object result) {
            this.result = result;
        }

        public Object getResult() {
            return result;
        }
    }
}
