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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;

@NodeField(name = "nonVectorPreserved", type = boolean.class)
public abstract class CastToVectorNode extends CastNode {

    public abstract Object executeObject(VirtualFrame frame, Object value);

    public abstract boolean isNonVectorPreserved();

    protected boolean preserveNonVector() {
        return isNonVectorPreserved();
    }

    @Specialization(guards = "preserveNonVector")
    @SuppressWarnings("unused")
    protected RNull castNull(RNull rnull) {
        return RNull.instance;
    }

    @Specialization(guards = "!preserveNonVector")
    @SuppressWarnings("unused")
    protected RAbstractVector cast(RNull rnull) {
        return RDataFactory.createList();
    }

    @Specialization(guards = "preserveNonVector")
    protected RFunction castFunction(RFunction f) {
        return f;
    }

    @Specialization(guards = "!preserveNonVector")
    @SuppressWarnings("unused")
    protected RAbstractVector cast(RFunction f) {
        return RDataFactory.createList();
    }

    @Specialization
    protected RAbstractVector cast(RAbstractVector vector) {
        return vector;
    }

}
