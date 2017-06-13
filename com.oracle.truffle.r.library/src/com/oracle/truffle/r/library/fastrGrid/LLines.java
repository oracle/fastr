/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LLines extends RExternalBuiltinNode.Arg4 {
    @Child private GridLinesNode gridLinesNode = GridLinesNode.createLines();

    static {
        Casts casts = new Casts(LLines.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(RList.class);
        casts.arg(2).allowNull().mustBe(RList.class);
    }

    public static LLines create() {
        return LLinesNodeGen.create();
    }

    @Specialization
    Object doLines(RAbstractVector x, RAbstractVector y, RList lengths, @SuppressWarnings("unused") RNull arrowIgnored) {
        gridLinesNode.execute(x, y, lengths, null);
        return RNull.instance;
    }

    @Specialization
    Object doLines(RAbstractVector x, RAbstractVector y, RList lengths, RList arrow) {
        gridLinesNode.execute(x, y, lengths, arrow);
        return RNull.instance;
    }
}
