/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;

final class ForcePromiseNode extends RNode {

    @Child private RNode valueNode;
    @Child private PromiseHelperNode promiseHelper;
    private final BranchProfile nonPromiseProfile = BranchProfile.create();

    ForcePromiseNode(RNode valueNode) {
        this.valueNode = valueNode;
    }

    public RNode getValueNode() {

        return valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object value = valueNode.execute(frame);
        if (value instanceof RPromise) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            return promiseHelper.evaluate(frame, (RPromise) value);
        } else {
            nonPromiseProfile.enter();
            return value;
        }
    }
}

@TypeSystemReference(RTypes.class)
@Instrumentable(factory = com.oracle.truffle.r.nodes.function.RCallBaseNodeWrapperFactory.class)
public abstract class RCallBaseNode extends RNode implements RInstrumentableNode {

    public abstract Object execute(VirtualFrame frame, Object function);

}
