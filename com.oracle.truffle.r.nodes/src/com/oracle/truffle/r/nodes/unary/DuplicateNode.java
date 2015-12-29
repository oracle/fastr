/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class DuplicateNode extends RBaseNode {

    public abstract Object executeObject(Object o);

    private final boolean deep;

    public DuplicateNode(boolean deep) {
        this.deep = deep;
    }

    // TODO: should we distinguish scalars to avoid conversion and copy?
    @Specialization
    protected Object duplicate(RAbstractVector vector) {
        return deep ? vector.deepCopy() : vector.copy();
    }

    @Specialization
    protected Object duplicate(RS4Object object) {
        RS4Object newObject = RDataFactory.createS4Object();
        RAttributes newAttributes = newObject.initAttributes();
        for (RAttribute attr : object.getAttributes()) {
            newAttributes.put(attr.getName(), attr.getValue());
        }
        return newObject;
    }

    @Specialization
    protected Object duplicate(RFunction f) {
        return f.copy();
    }

    // TODO: support more types when required
    @Fallback
    protected Object duplicate(@SuppressWarnings("unused") Object object) {
        throw RInternalError.unimplemented("duplication not supported");
    }

}
