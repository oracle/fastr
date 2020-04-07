/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix.special;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.runtime.data.nodes.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.ProfiledSpecialsUtilsFactory.ProfiledSubscriptSpecialNodeGen;
import com.oracle.truffle.r.nodes.helpers.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

public abstract class SubscriptSpecial extends AccessSpecial {

    protected SubscriptSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)", "!inReplacement"})
    protected static Object access(RList vector, int index,
                    @Cached("create()") ExtractListElement extract) {
        return extract.execute(vector, index - 1);
    }

    protected static ExtractVectorNode createAccess() {
        return ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);
    }

    @Specialization(guards = {"simpleVector(vector)", "!inReplacement"})
    protected static Object accessObject(RAbstractVector vector, Object index,
                    @Cached("createAccess()") ExtractVectorNode extract) {
        return extract.apply(vector, new Object[]{index}, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE);
    }

    public static RNode create(boolean inReplacement, RNode profiledVector, ConvertIndex index) {
        return ProfiledSubscriptSpecialNodeGen.create(inReplacement, profiledVector, index);
    }
}
