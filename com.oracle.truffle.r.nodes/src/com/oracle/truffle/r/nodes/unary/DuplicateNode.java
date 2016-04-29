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
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class DuplicateNode extends RBaseNode {

    public abstract Object executeObject(Object o);

    private final boolean deep;

    protected DuplicateNode(boolean deep) {
        this.deep = deep;
    }

    // TODO: should we distinguish scalars to avoid conversion and copy?
    @Specialization
    protected RAbstractVector duplicate(RAbstractVector vector) {
        return deep ? vector.deepCopy() : vector.copy();
    }

    @Specialization
    protected RS4Object duplicate(RS4Object object) {
        return object.copy();
    }

    @Specialization
    protected RFunction duplicate(RFunction f) {
        return f.copy();
    }

    @Specialization
    protected RExternalPtr duplicate(RExternalPtr p) {
        return p.copy();
    }

    @Specialization
    protected RExpression duplicate(RExpression e) {
        return e.copy();
    }

    @Specialization
    protected RLanguage duplicate(RLanguage l) {
        return l.copy();
    }

    @Specialization
    protected REnvironment duplicate(REnvironment e) {
        return e;
    }

    // TODO: support more types when required
    @Fallback
    protected Object duplicate(@SuppressWarnings("unused") Object object) {
        throw RInternalError.unimplemented("duplication not supported");
    }
}
