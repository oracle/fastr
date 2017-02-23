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
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class FilterNode extends CastNode {

    private final ArgumentFilter<Object, Object> filter;
    private final MessageData message;
    private final boolean isWarning;
    private final boolean resultForNull;
    private final boolean resultForMissing;

    private final BranchProfile warningProfile = BranchProfile.create();
    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    @Child private BoxPrimitiveNode boxPrimitiveNode;

    protected FilterNode(ArgumentFilter<Object, Object> filter, boolean isWarning, MessageData message, boolean boxPrimitives, boolean resultForNull, boolean resultForMissing) {
        this.filter = filter;
        this.isWarning = isWarning;
        assert message != null;
        this.message = message;
        this.boxPrimitiveNode = boxPrimitives ? BoxPrimitiveNodeGen.create() : null;
        this.resultForNull = resultForNull;
        this.resultForMissing = resultForMissing;
    }

    public static FilterNode create(ArgumentFilter<Object, Object> filter, boolean isWarning, MessageData message, boolean boxPrimitives, boolean resultForNull, boolean resultForMissing) {
        return FilterNodeGen.create(filter, isWarning, message, boxPrimitives, resultForNull, resultForMissing);
    }

    private void handleMessage(Object x) {
        if (isWarning) {
            if (message != null) {
                warningProfile.enter();
                handleArgumentWarning(x, message);
            }
        } else {
            throw handleArgumentError(x, message);
        }
    }

    @Specialization
    protected Object executeNull(RNull x) {
        if (!resultForNull) {
            handleMessage(x);
        }
        return x;
    }

    @Specialization
    protected Object executeMissing(RMissing x) {
        if (!resultForMissing) {
            handleMessage(x);
        }
        return x;
    }

    @Specialization(guards = {"!isRNull(x)", "!isRMissing(x)"})
    public Object executeRest(Object x,
                    @Cached("createBinaryProfile()") ConditionProfile conditionProfile) {
        if (!conditionProfile.profile(evalCondition(valueProfile.profile(x)))) {
            handleMessage(x);
        }
        return x;
    }

    protected boolean evalCondition(Object x) {
        Object y = boxPrimitiveNode != null ? boxPrimitiveNode.execute(x) : x;
        return filter.test(y);
    }
}
