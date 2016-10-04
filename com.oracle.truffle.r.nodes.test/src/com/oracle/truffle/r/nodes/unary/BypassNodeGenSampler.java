/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.casts.ArgumentMapperSampler;
import com.oracle.truffle.r.nodes.casts.CastNodeSampler;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.nodes.casts.TypeExpr;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BypassNodeGenSampler extends CastNodeSampler<BypassNodeGen> {

    private final CastNodeSampler wrappedHeadSampler;
    private final ArgumentMapperSampler nullMapper;
    private final ArgumentMapperSampler missingMapper;

    public BypassNodeGenSampler(BypassNodeGen bypassNode) {
        super(bypassNode);
        this.wrappedHeadSampler = bypassNode.getWrappedHead() == null ? null : createSampler(bypassNode.getWrappedHead());

        assert bypassNode.getNullMapper() == null || bypassNode.getNullMapper() instanceof ArgumentMapperSampler;
        assert bypassNode.getMissingMapper() == null || bypassNode.getMissingMapper() instanceof ArgumentMapperSampler;

        this.nullMapper = (ArgumentMapperSampler) bypassNode.getNullMapper();
        this.missingMapper = (ArgumentMapperSampler) bypassNode.getMissingMapper();
    }

    @Override
    public TypeExpr resultTypes(TypeExpr inputTypes) {
        TypeExpr rt = wrappedHeadSampler == null ? TypeExpr.ANYTHING : wrappedHeadSampler.resultTypes(inputTypes);
        if (nullMapper != null) {
            rt = rt.or(nullMapper.resultTypes(inputTypes));
        } else {
            rt = rt.and(TypeExpr.atom(RNull.class).not());
        }
        if (missingMapper != null) {
            rt = rt.or(missingMapper.resultTypes(inputTypes));
        } else {
            rt = rt.and(TypeExpr.atom(RMissing.class).not());
        }
        return rt;
    }

    @Override
    public Samples<?> collectSamples(TypeExpr inputType, Samples<?> downStreamSamples) {
        Samples<?> samples = wrappedHeadSampler == null ? Samples.nothing() : wrappedHeadSampler.collectSamples(inputType, downStreamSamples);
        if (nullMapper != null) {
            samples = samples.or(nullMapper.collectSamples(downStreamSamples));
        }
        if (missingMapper != null) {
            samples = samples.or(missingMapper.collectSamples(downStreamSamples));
        }
        return samples;
    }

}
