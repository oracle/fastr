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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.env.REnvironment;

@RBuiltin(name = "names", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC)
public abstract class Names extends RBuiltinNode {

    private final ConditionProfile hasNames = ConditionProfile.createBinaryProfile();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization
    protected Object getNames(RAbstractContainer container) {
        if (hasNames.profile(container.getNames(attrProfiles) != null)) {
            return container.getNames(attrProfiles);
        } else {
            return RNull.instance;
        }
    }

    @Specialization
    @TruffleBoundary
    protected Object getNames(REnvironment env) {
        return env.ls(true, null, false);
    }

    @Fallback
    protected RNull getNames(@SuppressWarnings("unused") Object operand) {
        return RNull.instance;
    }
}
