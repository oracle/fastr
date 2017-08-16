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
package com.oracle.truffle.r.nodes.test;

import com.oracle.truffle.r.nodes.builtin.casts.CastForeignNode;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.builtin.casts.analysis.ForwardingAnalysisResult;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.unary.CastIntegerBaseNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalBaseNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringBaseNode;
import com.oracle.truffle.r.nodes.unary.ChainedCastNode;
import com.oracle.truffle.r.nodes.unary.FilterNode;
import com.oracle.truffle.r.nodes.unary.FindFirstNode;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Tests that {@link PipelineToCastNode} converts manually constructed steps into the correct cast
 * nodes.
 */
public class PipelineToCastNodeTests {
    @Test
    public void asLogicalVector() {
        // without first cast of foreign objects
        CastNode pipeline = createPipeline(new CoercionStep<>(RType.Logical, false), false);
        assertTrue(pipeline instanceof CastLogicalBaseNode);

        // with first cast of foreign objects
        pipeline = createPipeline(new CoercionStep<>(RType.Logical, false), true);
        assertChainedCast(pipeline, CastForeignNode.class, CastLogicalBaseNode.class);
    }

    @Test
    public void asStringVectorFindFirst() {
        PipelineStep<?, ?> steps = new CoercionStep<>(RType.Character, false).setNext(new FindFirstStep<>("hello", String.class, null));

        // without first cast of foreign objects
        CastNode pipeline = createPipeline(steps, false);
        assertChainedCast(pipeline, CastStringBaseNode.class, FindFirstNode.class);

        FindFirstNode findFirst = (FindFirstNode) ((ChainedCastNode) pipeline).getSecondCast();
        assertEquals("hello", findFirst.getDefaultValue());

        // with first cast of foreign objects
        pipeline = createPipeline(steps, true);
        assertChainedCast(pipeline, ChainedCastNode.class, FindFirstNode.class);

        CastNode next = ((ChainedCastNode) pipeline).getFirstCast();
        assertChainedCast(next, CastForeignNode.class, CastStringBaseNode.class);
    }

    @Test
    public void mustBeREnvironmentAsIntegerVectorFindFirst() {
        PipelineStep<?, ?> steps = new FilterStep<>(new TypeFilter<>(REnvironment.class), null, false).setNext(
                        new CoercionStep<>(RType.Integer, false).setNext(new FindFirstStep<>("hello", String.class, null)));

        // without first cast of foreign objects
        CastNode pipeline = createPipeline(steps, false);

        assertChainedCast(pipeline, ChainedCastNode.class, FindFirstNode.class);
        CastNode next = ((ChainedCastNode) pipeline).getFirstCast();
        assertChainedCast(next, FilterNode.class, CastIntegerBaseNode.class);

        FindFirstNode findFirst = (FindFirstNode) ((ChainedCastNode) pipeline).getSecondCast();
        assertEquals("hello", findFirst.getDefaultValue());

        // with first cast of foreign objects
        pipeline = createPipeline(steps, true);
        assertChainedCast(pipeline, ChainedCastNode.class, FindFirstNode.class);

        next = ((ChainedCastNode) pipeline).getFirstCast();
        assertChainedCast(next, ChainedCastNode.class, CastIntegerBaseNode.class);

        next = ((ChainedCastNode) next).getFirstCast();
        assertChainedCast(next, CastForeignNode.class, FilterNode.class);

    }

    private static void assertChainedCast(CastNode node, Class<?> expectedFirst, Class<?> expectedSecond) {
        assertTrue(node instanceof ChainedCastNode);
        assertTrue(expectedFirst.isInstance(((ChainedCastNode) node).getFirstCast()));
        assertTrue(expectedSecond.isInstance(((ChainedCastNode) node).getSecondCast()));
    }

    private static CastNode createPipeline(PipelineStep<?, ?> lastStep, boolean castForeign) {
        PipelineConfigBuilder configBuilder = new PipelineConfigBuilder("x");
        configBuilder.setValueForwarding(false);
        configBuilder.setCastForeignObjects(castForeign);
        return PipelineToCastNode.convert(configBuilder.build(), lastStep, ForwardingAnalysisResult.INVALID);
    }
}
