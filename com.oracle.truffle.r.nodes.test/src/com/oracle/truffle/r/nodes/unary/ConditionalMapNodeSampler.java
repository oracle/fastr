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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.r.nodes.casts.ArgumentFilterSampler;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.TypeExpr;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ConditionalMapNodeSampler extends CastNodeSampler<ConditionalMapNode> {

    private final ArgumentFilterSampler argFilter;
    private final CastNodeSampler trueBranch;
    private final CastNodeSampler falseBranch;

    public ConditionalMapNodeSampler(ConditionalMapNode castNode) {
        super(castNode);
        argFilter = (ArgumentFilterSampler) castNode.getFilter();
        trueBranch = createSampler(castNode.getTrueBranch());
        falseBranch = createSampler(castNode.getFalseBranch());
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputType) {
        return trueBranchResultTypes(inputType).or(falseBranchResultTypes(inputType));
    }

    private TypeExpr trueBranchResultTypes(TypeExpr inputType) {
        return trueBranch.resultTypes(argFilter.trueBranchType().and(inputType));
    }

    private TypeExpr falseBranchResultTypes(TypeExpr inputType) {
        if (falseBranch != null) {
            return falseBranch.resultTypes(argFilter.falseBranchType().and(inputType));
        } else {
            return argFilter.falseBranchType().and(inputType);
        }
    }

    @Override
    public Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
        TypeExpr trueBranchResultType = trueBranchResultTypes(inputType);
        TypeExpr falseBranchResultType = falseBranchResultTypes(inputType);

        // filter out the incompatible samples
        Samples compatibleTrueBranchDownStreamSamples = downStreamSamples.filter(x -> trueBranchResultType.isInstance(x));
        Samples compatibleFalseBranchDownStreamSamples = downStreamSamples.filter(x -> falseBranchResultType.isInstance(x));

        Samples trueBranchSamples = trueBranch.collectSamples(argFilter.trueBranchType().and(inputType), compatibleTrueBranchDownStreamSamples);
        Samples falseBranchSamples = falseBranch == null ? compatibleFalseBranchDownStreamSamples
                        : falseBranch.collectSamples(argFilter.falseBranchType().and(inputType), compatibleFalseBranchDownStreamSamples);
        Samples bothBranchesSamples = trueBranchSamples.or(falseBranchSamples);

        // Collect the "interesting" samples from the condition. Both positive and negative samples
        // are actually interpreted as positive ones.
        Samples origConditionSamples = argFilter.collectSamples(inputType).makePositive();
        // Merge the samples from the branches with the condition samples
        return origConditionSamples.or(bothBranchesSamples);
    }
}
