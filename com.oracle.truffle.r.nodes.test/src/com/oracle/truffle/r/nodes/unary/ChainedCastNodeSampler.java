/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.TypeExpr;

public final class ChainedCastNodeSampler extends CastNodeSampler<ChainedCastNode> {

    private final CastNodeSampler<?> firstCast;
    private final CastNodeSampler<?> secondCast;

    public ChainedCastNodeSampler(ChainedCastNode castNode) {
        super(castNode);

        firstCast = createSampler(castNode.getFirstCast());
        secondCast = createSampler(castNode.getSecondCast());
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputTypes, SamplingContext ctx) {
        return secondCast.resultTypes(firstCast.resultTypes(inputTypes, ctx), ctx);
    }

    @Override
    public String toString() {
        return firstCast.toString();
    }

    @Override
    public Samples<?> collectSamples(TypeExpr inputTypes, Samples<?> downStreamSamples) {
        TypeExpr rt1 = firstCast.resultTypes(inputTypes, new SamplingContext());
        return firstCast.collectSamples(inputTypes, secondCast.collectSamples(rt1, downStreamSamples));
    }
}
