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

@SuppressWarnings("rawtypes")
public class FilterNodeGenSampler extends CastNodeSampler<FilterNodeGen> {

    private final ArgumentFilterSampler filter;
    private final boolean isWarning;
    private final TypeExpr resType;

    public FilterNodeGenSampler(FilterNodeGen castNode) {
        super(castNode);
        assert castNode.getFilter() instanceof ArgumentFilterSampler : "Check PredefFiltersSamplers is installed in Predef";
        this.filter = (ArgumentFilterSampler) castNode.getFilter();
        this.isWarning = castNode.isWarning();
        this.resType = filter.trueBranchType();
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputType) {
        if (isWarning) {
            return inputType;
        } else {
            return inputType.and(resType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
        if (isWarning) {
            return downStreamSamples;
        } else {
            Samples samples = filter.collectSamples(inputType);

            Samples<?> combined = samples.and(downStreamSamples);
            return combined;
        }
    }
}
