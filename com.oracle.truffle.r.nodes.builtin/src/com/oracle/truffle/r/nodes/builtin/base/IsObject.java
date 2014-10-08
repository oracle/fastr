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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The specification is not quite what you might expect. Several builtin types, e.g.,
 * {@code expression} respond to {@code class(e)} but return {@code FALSE} to {@code is.object}.
 * Essentially, this method should only return {@code TRUE} if a {@code class} attribute has been
 * added explicitly to the object. If the attribute is removed, it shoukld return {@code FALSE}.
 */
@RBuiltin(name = "is.object", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class IsObject extends RBuiltinNode {

    @Specialization
    @SuppressWarnings("unused")
    protected byte isObject(RNull arg) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isObject(RAbstractContainer arg) {
        controlVisibility();
        return arg.isObject() ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte isObject(@SuppressWarnings("unused") RConnection conn) {
        // No need to enquire, connections always have a class attribute.
        return RRuntime.LOGICAL_TRUE;
    }
}
