/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild("condition"), @NodeChild("thenPart"), @NodeChild("elsePart")})
@NodeField(name = "elseGiven", type = boolean.class)
public abstract class IfNode extends RNode implements VisibilityController {

    protected abstract boolean isElseGiven();

    /**
     * Result visibility of an if node depends on whether there is an else branch or not, and on the
     * condition. For instance, the expression {@code if (FALSE) 23} will evaluate to {@code NULL},
     * but the result will not be printed in the shell. Conversely, {@code NULL} will be printed for
     * {@code if (FALSE) 23 else NULL} because the else branch is given.
     *
     * This means that we need to take care of visibility in this class, and do a double check of
     * the condition and the presence of an else branch below in {@link #doObject}.
     */
    private boolean isVisible;

    @Override
    public boolean getVisibility() {
        return isVisible;
    }

    public static RNode create(RNode condition, RNode thenPart, RNode elsePart) {
        if (elsePart != null) {
            return IfNodeFactory.create(condition, thenPart, elsePart, true);
        } else {
            return IfNodeFactory.create(condition, thenPart, ConstantNode.create(RNull.instance), false);
        }
    }

    public static RNode create(SourceSection src, RNode condition, RNode thenPart, RNode elsePart) {
        RNode i = create(condition, thenPart, elsePart);
        i.assignSourceSection(src);
        return i;
    }

    @CreateCast({"condition"})
    public ConvertBooleanNode conditionToBoolean(RNode node) {
        return ConvertBooleanNode.create(node);
    }

    @ShortCircuit("thenPart")
    public static boolean needsThenPart(byte condition) {
        return condition == RRuntime.LOGICAL_TRUE;
    }

    @ShortCircuit("elsePart")
    public static boolean needsElsePart(byte condition, boolean hasThenPart, Object value) {
        return !hasThenPart;
    }

    @Specialization
    public Object doObject(byte condition, boolean hasThen, Object left, boolean hasElse, Object right) {
        isVisible = condition == RRuntime.LOGICAL_TRUE || isElseGiven();
        controlVisibility();
        return hasThen ? left : right;
    }
}
