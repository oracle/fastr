/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.ffi.impl.common.RFFIUtils.guaranteeInstanceOf;

import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElementNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class VectorElementGetterNode extends RBaseNode {

    @Child private ExtractListElement extractNode = ExtractListElementNodeGen.create();

    public Object executeObject(Object x, long i) {
        Object vec = x;
        if (vec instanceof RExpression) {
            return ((RExpression) vec).getDataAt((int) i);
        }
        RAbstractListVector list = guaranteeInstanceOf(RRuntime.asAbstractVector(vec), RAbstractListVector.class);
        if (list.getLength() == i) {
            // Some packages abuse that there seems to be no bounds checking and the
            // one-after-the-last element returns NULL, which they use to find out if they reached
            // the end of the list...
            return RNull.instance;
        }
        return extractNode.execute(list, (int) i);
    }

}
