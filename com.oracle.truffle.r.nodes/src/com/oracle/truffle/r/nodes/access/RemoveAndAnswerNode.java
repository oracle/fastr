/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;

/**
 * This node removes a slot from the current frame (i.e., sets it to {@code null} to allow fast-path
 * usage) and returns the slot's value. The node must be used with extreme caution as it does not
 * perform checking; it is to be used for internal purposes. A sample use case is a
 * {@linkplain RTruffleVisitor#visit(Replacement) replacement}.
 */
public final class RemoveAndAnswerNode extends RNode implements VisibilityController {

    /**
     * The name of the variable that is to be removed and whose value is to be returned.
     */
    private final String name;

    private RemoveAndAnswerNode(String name) {
        this.name = name;
    }

    public static RemoveAndAnswerNode create(String name) {
        return new RemoveAndAnswerNode(name);
    }

    public static RemoveAndAnswerNode create(Object name) {
        return new RemoveAndAnswerNode(name.toString());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        controlVisibility();
        FrameSlot fs = frame.getFrameDescriptor().findFrameSlot(name);
        if (fs == null) {
            RError.warning(this.getEncapsulatingSourceSection(), RError.Message.UNKNOWN_OBJECT, name);
        }
        Object result = frame.getValue(fs);
        frame.setObject(fs, null); // use null (not an R value) to represent "undefined"
        return result;
    }

    @Override
    public boolean getVisibility() {
        return false;
    }

}
