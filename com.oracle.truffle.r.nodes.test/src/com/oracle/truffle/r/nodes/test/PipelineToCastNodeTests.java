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
package com.oracle.truffle.r.nodes.test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.r.nodes.builtin.casts.Filter.RTypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Filter.TypeFilter;
import com.oracle.truffle.r.nodes.builtin.casts.Mapper.MapByteToBoolean;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.CoercionStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FilterStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.FindFirstStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep.MapStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.unary.BypassNode;
import com.oracle.truffle.r.nodes.unary.BypassNode.BypassIntegerNode;
import com.oracle.truffle.r.nodes.unary.BypassNode.BypassLogicalMapToBooleanNode;
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
        CastNode pipeline = createPipeline(new CoercionStep<>(RType.Logical, false));
        CastNode castNode = assertBypassNode(pipeline);
        assertTrue(castNode instanceof CastLogicalBaseNode);
    }

    @Test
    public void asStringVectorFindFirst() {
        CastNode pipeline = createPipeline(new CoercionStep<>(RType.Character, false).setNext(new FindFirstStep<>("hello", String.class, null)));
        CastNode chain = assertBypassNode(pipeline);
        assertChainedCast(chain, CastStringBaseNode.class, FindFirstNode.class);
        FindFirstNode findFirst = (FindFirstNode) ((ChainedCastNode) chain).getSecondCast();
        assertEquals("hello", findFirst.getDefaultValue());
    }

    @Test
    public void optimizeForSingleInteger() {
        // should be equivalent of mustBe(integerValue()).asIntegerVector().findFirst()
        CastNode pipeline = createPipeline(
                        new FilterStep<>(new RTypeFilter<>(RType.Integer), null, false).setNext(new CoercionStep<>(RType.Integer, false).setNext(new FindFirstStep<>(null, Integer.class, null))));
        assertBypassNode(pipeline, BypassIntegerNode.class);
    }

    @Test
    public void optimizeMapSingleByteToBoolean() {
        // should be equivalent of mustBe(integerValue()).asIntegerVector().findFirst()
        CastNode pipeline = createPipeline(new CoercionStep<>(RType.Logical, false).setNext(new FindFirstStep<>(null, Byte.class, null).setNext(new MapStep<>(MapByteToBoolean.INSTANCE))));
        assertBypassNode(pipeline, BypassLogicalMapToBooleanNode.class);
    }

    @Test
    public void mustBeREnvironmentAsIntegerVectorFindFirst() {
        CastNode pipeline = createPipeline(new FilterStep<>(new TypeFilter<>(x -> x instanceof REnvironment, REnvironment.class), null, false).setNext(
                        new CoercionStep<>(RType.Integer, false).setNext(new FindFirstStep<>("hello", String.class, null))));
        CastNode chain = assertBypassNode(pipeline);
        assertChainedCast(chain, ChainedCastNode.class, FindFirstNode.class);
        CastNode next = ((ChainedCastNode) chain).getFirstCast();
        assertChainedCast(next, FilterNode.class, CastIntegerBaseNode.class);
        FindFirstNode findFirst = (FindFirstNode) ((ChainedCastNode) chain).getSecondCast();
        assertEquals("hello", findFirst.getDefaultValue());
    }

    private static CastNode assertBypassNode(CastNode node) {
        assertTrue(node instanceof BypassNode);
        return ((BypassNode) node).getWrappedHead();
    }

    private static CastNode assertBypassNode(CastNode node, Class<?> expectedClass) {
        assertTrue(expectedClass.isInstance(node));
        return ((BypassNode) node).getWrappedHead();
    }

    private static void assertChainedCast(CastNode node, Class<?> expectedFirst, Class<?> expectedSecond) {
        assertTrue(node instanceof ChainedCastNode);
        assertTrue(expectedFirst.isInstance(((ChainedCastNode) node).getFirstCast()));
        assertTrue(expectedSecond.isInstance(((ChainedCastNode) node).getSecondCast()));
    }

    private static CastNode createPipeline(PipelineStep<?, ?> lastStep) {
        PipelineConfigBuilder configBuilder = new PipelineConfigBuilder("x");
        return PipelineToCastNode.convert(configBuilder.build(), lastStep);
    }
}
