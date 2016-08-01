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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.ArgumentFilter;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class FilterNode extends CastNode {

    private final ArgumentFilter filter;
    private final RError.Message message;
    private final RBaseNode callObj;
    private final Object[] messageArgs;
    private final boolean boxPrimitives;
    private final boolean isWarning;

    private final BranchProfile warningProfile = BranchProfile.create();

    @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNodeGen.create();

    protected FilterNode(ArgumentFilter<?, ?> filter, boolean isWarning, RBaseNode callObj, RError.Message message, Object[] messageArgs, boolean boxPrimitives) {
        this.filter = filter;
        this.isWarning = isWarning;
        this.callObj = callObj == null ? this : callObj;
        this.message = message;
        this.messageArgs = messageArgs;
        this.boxPrimitives = boxPrimitives;
    }

    public ArgumentFilter getFilter() {
        return filter;
    }

    public boolean isWarning() {
        return isWarning;
    }

    private void handleMessage(Object x) {
        if (isWarning) {
            if (message != null) {
                warningProfile.enter();
                handleArgumentWarning(x, callObj, message, messageArgs);
            }
        } else {
            handleArgumentError(x, callObj, message, messageArgs);
        }
    }

    @Specialization(guards = "evalCondition(x)")
    protected Object onTrue(Object x) {
        return x;
    }

    @Fallback
    protected Object onFalse(Object x) {
        handleMessage(x);
        return x;
    }

    protected boolean evalCondition(Object x) {
        Object y = boxPrimitives ? boxPrimitiveNode.execute(x) : x;
        return filter.test(y);
    }

}
