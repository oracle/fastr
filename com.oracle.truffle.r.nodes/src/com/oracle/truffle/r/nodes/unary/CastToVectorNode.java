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

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@NodeField(name = "preserveNonVector", type = boolean.class)
public abstract class CastToVectorNode extends CastNode {

    public abstract boolean isPreserveNonVector();

    @Specialization
    protected Object castNull(@SuppressWarnings("unused") RNull rnull) {
        if (isPreserveNonVector()) {
            return RNull.instance;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected Object castMissing(@SuppressWarnings("unused") RMissing missing) {
        if (isPreserveNonVector()) {
            return RMissing.instance;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected Object castFunction(RFunction f) {
        if (isPreserveNonVector()) {
            return f;
        } else {
            return RDataFactory.createList();
        }
    }

    @Specialization
    protected RAbstractVector cast(RAbstractVector vector) {
        return vector;
    }

    @Specialization
    protected RList cast(RExpression expression) {
        return expression.getList();
    }

    public static CastToVectorNode create() {
        return CastToVectorNodeGen.create(false);
    }
}
