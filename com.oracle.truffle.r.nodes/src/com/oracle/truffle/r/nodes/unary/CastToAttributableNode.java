/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class CastToAttributableNode extends CastBaseNode {

    public abstract Object executeObject(Object value);

    protected CastToAttributableNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        this(preserveNames, preserveDimensions, preserveAttributes, false);
    }

    protected CastToAttributableNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes, boolean forRFFI) {
        super(preserveNames, preserveDimensions, preserveAttributes, forRFFI);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Any;
    }

    @Specialization
    protected RNull cast(@SuppressWarnings("unused") RNull rnull) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing cast(@SuppressWarnings("unused") RMissing rmissing) {
        return RMissing.instance;
    }

    // This specialization causes boxing of primitive types to a container (via Truffle DSL)
    @Specialization
    protected RAttributable cast(RAbstractContainer container) {
        return container;
    }

    @Specialization(guards = "!isRAbstractContainer(x)")
    protected RAttributable cast(RAttributable x) {
        return x;
    }
}
