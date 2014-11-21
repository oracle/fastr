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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "names", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class Names extends RBuiltinNode {

    private ConditionProfile hasNames = ConditionProfile.createBinaryProfile();
    private ConditionProfile hasDimNames = ConditionProfile.createBinaryProfile();

    @Specialization
    protected Object getNames(RAbstractContainer container) {
        controlVisibility();
        if (hasNames.profile(container.getNames() != null && container.getNames() != RNull.instance)) {
            return container.getNames();
        } else if (hasDimNames.profile(container.getDimNames() != null && container.getDimNames().getLength() == 1)) {
            return container.getDimNames().getDataAt(0);
        } else {
            return RNull.instance;
        }
    }

    @Fallback
    protected RNull getNames(@SuppressWarnings("unused") Object operand) {
        controlVisibility();
        return RNull.instance;
    }

}
