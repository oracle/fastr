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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PipelineConfigBuilder;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineStep;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.BypassNodeGen.BypassIntegerNodeGen;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * The node wraps cast pipeline and handles {@code RNull} and {@code RMissing} according to
 * {@link PipelineConfigBuilder}. If the pipeline contains {@code findFirst} step and RNull/RMissing
 * is allowed in the config, then RNull/RMissing is routed to the logic of {@code findFirst}, i.e.
 * without defaultValue, it gives error, with defaultValue, returns the defaultValue. Any mappers
 * after findFirst will be applied too.
 *
 * The factory method {@link #create(PipelineConfigBuilder, CastNode)} creates either directly
 * {@link BypassNode} or one of its protected subclasses that can also bypass single atomic values
 * (these are also, like RNull/RMissing, routed to findFirst and any consecutive mappers). The
 * subclasses correspond to subclasses of {@link CastBaseNode}. The idea is that if the pipeline
 * until 'findFirst' contains only one 'asXYVector' step and no 'map' or 'mapIf', then we can assume
 * that any atomic value of type XY can be passed directly to the 'findFirst' step (although mustBe
 * could disallow values of type XY, we assume that this will not happen when asXYVector is used,
 * and any checks of the value will be done after findFirst).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class BypassNode extends CastNode {

    private final boolean isRNullBypassed;
    private final MessageData nullMsg;
    private final ArgumentMapper nullMapFn;

    private final boolean isRMissingBypassed;
    private final MessageData missingMsg;
    private final ArgumentMapper missingMapFn;
    private final boolean noHead;

    /**
     * This is the cast pipeline itself.
     */
    @Child private CastNode wrappedHead;

    /**
     * If there is a {@link FindFirstNode} in the pipeline, this will hold copy of it.
     */
    @Child private FindFirstNode directFindFirstNode;

    /**
     * If there are some steps after the {@link FindFirstNode} in the cast pipeline, then this will
     * hold copy of its first node (which can be chained to following nodes).
     */
    @Child private CastNode afterFindFirst;

    protected BypassNode(PipelineConfigBuilder pcb, CastNode wrappedHead, FindFirstNode directFindFirstNode, CastNode afterFindFirst) {
        this.nullMapFn = PipelineToCastNode.convert(pcb.getNullMapper());
        this.isRNullBypassed = this.nullMapFn != null;
        this.nullMsg = pcb.getNullMessage() == null ? null : pcb.getNullMessage().fixCallObj(this);

        this.missingMapFn = PipelineToCastNode.convert(pcb.getMissingMapper());
        this.isRMissingBypassed = this.missingMapFn != null;
        this.missingMsg = pcb.getMissingMessage() == null ? null : pcb.getMissingMessage().fixCallObj(this);

        this.wrappedHead = wrappedHead;
        this.noHead = wrappedHead == null;

        this.directFindFirstNode = insertIfNotNull(directFindFirstNode);
        this.afterFindFirst = insertIfNotNull(afterFindFirst);
    }

    public final CastNode getWrappedHead() {
        return wrappedHead;
    }

    public final ArgumentMapper getNullMapper() {
        return nullMapFn;
    }

    public final ArgumentMapper getMissingMapper() {
        return missingMapFn;
    }

    protected final Object executeAfterFindFirst(Object value) {
        if (directFindFirstNode != null) {
            return afterFindFirst.execute(value);
        } else {
            return value;
        }
    }

    private Object executeFindFirstPipeline(Object value) {
        Object result = directFindFirstNode.execute(value);
        if (afterFindFirst != null) {
            result = afterFindFirst.execute(result);
        }
        return result;
    }

    private <T extends Node> T insertIfNotNull(T child) {
        return child != null ? insert(child) : child;
    }

    @Specialization
    public Object bypassRNull(RNull x) {
        if (isRNullBypassed) {
            if (nullMsg != null) {
                handleArgumentWarning(x, nullMsg.getCallObj(), nullMsg.getMessage(), nullMsg.getMessageArgs());
            }
            return nullMapFn.map(x);
        } else if (directFindFirstNode != null) {
            return executeFindFirstPipeline(x);
        } else if (nullMsg == null) {
            // go to the pipeline
            return handleOthers(x);
        } else {
            handleArgumentError(x, nullMsg.getCallObj(), nullMsg.getMessage(), nullMsg.getMessageArgs());
            return x;
        }
    }

    @Specialization
    public Object bypassRMissing(RMissing x) {
        if (isRMissingBypassed) {
            if (missingMsg != null) {
                handleArgumentWarning(x, missingMsg.getCallObj(), missingMsg.getMessage(), missingMsg.getMessageArgs());
            }
            return missingMapFn.map(x);
        } else if (directFindFirstNode != null) {
            return executeFindFirstPipeline(x);
        } else if (missingMsg == null) {
            // go to the pipeline
            return handleOthers(x);
        } else {
            handleArgumentError(x, missingMsg.getCallObj(), missingMsg.getMessage(), missingMsg.getMessageArgs());
            return x;
        }
    }

    @Fallback
    public Object handleOthers(Object x) {
        return noHead ? x : wrappedHead.execute(x);
    }

    /**
     * Factory method that inspects the given cast pipeline and returns appropriate subclass of
     * {@link BypassNode} possibly optimized for a pattern found in the pipeline. See
     * {@link BypassNode} doc for details.
     */
    public static CastNode create(PipelineConfigBuilder pcb, CastNode wrappedHead) {
        if (wrappedHead == null) {
            return BypassNodeGen.create(pcb, wrappedHead, null, null);
        }

        // Here we traverse the cast chain looking for FindFirstNode, if we find it, we continue
        // traversing to see if there is only single asXYVector step
        boolean foundFindFirst = false;
        FindFirstNode directFindFirstNode = null;
        CastNode afterFindFirstNode = null;
        ChainedCastNode previousCurrent = null;
        CastNode current = wrappedHead;
        Class singleCastBaseNodeClass = null;   // represents the single asXYVector step
        while (current instanceof ChainedCastNode) {
            ChainedCastNode currentChained = (ChainedCastNode) current;
            CastNode currentSecond = currentChained.getSecondCast();

            if (!foundFindFirst && currentSecond instanceof FindFirstNode) {
                foundFindFirst = true;
                if (((FindFirstNode) currentSecond).getDefaultValue() != null) {
                    // we are only interested in 'findFirst' with some default value in order to map
                    // RNull/RMissing to it.
                    directFindFirstNode = (FindFirstNode) currentChained.getSecondCastFact().create();
                }
                if (previousCurrent != null) {
                    afterFindFirstNode = previousCurrent.getSecondCastFact().create();
                }
            } else if (foundFindFirst && currentSecond instanceof CastBaseNode) {
                if (singleCastBaseNodeClass != null) {
                    singleCastBaseNodeClass = null;
                    break;
                }
                singleCastBaseNodeClass = currentSecond.getClass();
            }

            previousCurrent = currentChained;
            current = currentChained.getFirstCast();
        }

        if (singleCastBaseNodeClass == null || !foundFindFirst) {
            return BypassNodeGen.create(pcb, wrappedHead, directFindFirstNode, afterFindFirstNode);
        }

        return createBypassByClass(pcb, wrappedHead, directFindFirstNode, afterFindFirstNode, singleCastBaseNodeClass);
    }

    /**
     * Depending on the {@code bypassClass} parameter creates corresponding {@code BypassXYNode}
     * instance.
     */
    private static BypassNode createBypassByClass(PipelineConfigBuilder pcb, CastNode wrappedHead, FindFirstNode directFindFirstNode, CastNode afterFindFirstNode, Class castNodeClass) {
        if (castNodeClass == CastIntegerNode.class) {
            return BypassIntegerNodeGen.create(pcb, wrappedHead, directFindFirstNode, afterFindFirstNode);
        } else if (castNodeClass == CastDoubleBaseNode.class) {
            return BypassDoubleNodeGen.create(pcb, wrappedHead, directFindFirstNode, afterFindFirstNode);
        } else {
            return BypassNodeGen.create(pcb, wrappedHead, directFindFirstNode, afterFindFirstNode);
        }
    }

    protected abstract static class BypassIntegerNode extends BypassNode {
        protected BypassIntegerNode(PipelineConfigBuilder pcb, CastNode wrappedHead, FindFirstNode directFindFirstNode, CastNode afterFindFirst) {
            super(pcb, wrappedHead, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        protected Object bypassInteger(int x) {
            return executeAfterFindFirst(x);
        }
    }

    protected abstract static class BypassDoubleNode extends BypassNode {
        protected BypassDoubleNode(PipelineConfigBuilder pcb, CastNode wrappedHead, FindFirstNode directFindFirstNode, CastNode afterFindFirst) {
            super(pcb, wrappedHead, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        protected Object bypassDouble(double x) {
            return executeAfterFindFirst(x);
        }
    }
}
