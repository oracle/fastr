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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Perform a field access. This node represents the {@code $} operator in R.
 */
@NodeChild(value = "object", type = RNode.class)
@NodeField(name = "field", type = String.class)
public abstract class AccessFieldNode extends RNode {

    public abstract String getField();

    private final BranchProfile inexactMatch = new BranchProfile();

    @Specialization(guards = "hasNames")
    protected Object accessField(RList object) {
        int index = object.getElementIndexByName(getField());
        if (index == -1) {
            inexactMatch.enter();
            index = object.getElementIndexByNameInexact(getField());
        }
        return index == -1 ? RNull.instance : object.getDataAt(index);
    }

    @Specialization(guards = "!hasNames")
    protected Object accessFieldNoNames(@SuppressWarnings("unused") RList object) {
        return RNull.instance;
    }

    @Specialization
    protected Object accessField(REnvironment env) {
        Object obj = env.get(getField());
        return obj == null ? RNull.instance : obj;
    }

    @Specialization
    protected Object accessField(@SuppressWarnings("unused") RAbstractVector object) {
        throw RError.error(RError.Message.DOLLAR_ATOMIC_VECTORS);
    }

    protected static boolean hasNames(RList object) {
        return object.getNames() != RNull.instance;
    }

}
