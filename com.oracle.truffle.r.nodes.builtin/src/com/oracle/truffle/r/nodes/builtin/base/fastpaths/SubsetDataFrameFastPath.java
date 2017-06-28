/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class SubsetDataFrameFastPath extends RFastPathNode {

    @Child private ExtractVectorNode extractNode = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);

    @Specialization(guards = {"positions.getLength() == 2", "positions.getSignature().getNonNullCount() == 0"})
    protected Object subscript2(VirtualFrame frame, RAbstractListVector df, RArgsValuesAndNames positions, Object exact,
                    @Cached("create()") AsScalarNode asScalar1,
                    @Cached("create()") AsScalarNode asScalar2) {
        Object pos2 = asScalar2.execute(positions.getArgument(1));
        if (pos2 == null) {
            return null;
        }
        Object extracted = extractNode.apply(frame, df, new Object[]{pos2}, exact, RRuntime.LOGICAL_TRUE);
        Object pos1 = asScalar1.execute(positions.getArgument(0));
        if (pos1 == null) {
            return null;
        }
        return extractNode.apply(frame, extracted, new Object[]{pos1}, exact, RRuntime.LOGICAL_TRUE);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected Object fallback(Object df, Object positions, Object exact) {
        return null;
    }
}
