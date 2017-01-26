/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.casts.MessageData;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineConfig;
import com.oracle.truffle.r.nodes.builtin.casts.PipelineToCastNode.ArgumentMapperFactory;
import com.oracle.truffle.r.nodes.unary.ConditionalMapNode.PipelineReturnException;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * The node wraps the cast node created for a cast pipeline and handles {@code RNull} and
 * {@code RMissing} according to {@link PipelineConfig}. Those values are either blocked or sent
 * directly to either the 'find first' node with default value if there is any 'find first' node
 * with default value in the pipeline or to directly to the builtin.
 *
 * There are several specialization capable of, on top of handling RNull/RMissing, sending a
 * primitive value, e.g. integer, directly the first node after the 'find first' thus bypassing any
 * other logic in between.
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
     * If there is a {@link FindFirstNode} in the pipeline with a default value, this will hold copy
     * of it.
     */
    @Child private CastNode directFindFirstNodeWithDefault;

    /**
     * If there are some steps after the {@link FindFirstNode} in the cast pipeline, then this will
     * hold copy of its first node (which can be chained to following nodes).
     */
    @Child private CastNode afterFindFirst;

    protected BypassNode(PipelineConfig conf, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNodeWithDefault,
                    CastNode afterFindFirst) {
        this.nullMapFn = conf.getNullMapper() == null ? null : mapperFactory.createMapper(conf.getNullMapper());
        this.isRNullBypassed = this.nullMapFn != null;
        this.nullMsg = getMessage(isRNullBypassed, conf.getNullMessage(), conf);

        this.missingMapFn = conf.getMissingMapper() == null ? null : mapperFactory.createMapper(conf.getMissingMapper());
        this.isRMissingBypassed = this.missingMapFn != null;
        this.missingMsg = getMessage(isRMissingBypassed, conf.getMissingMessage(), conf);

        this.wrappedHead = wrappedHead;
        this.noHead = wrappedHead == null;

        this.directFindFirstNodeWithDefault = insertIfNotNull(directFindFirstNodeWithDefault);
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
        if (afterFindFirst != null) {
            try {
                return afterFindFirst.execute(value);
            } catch (PipelineReturnException ret) {
                return ret.getResult();
            }
        } else {
            return value;
        }
    }

    private <T extends Node> T insertIfNotNull(T child) {
        return child != null ? insert(child) : child;
    }

    private MessageData getMessage(boolean isWarning, MessageData msg, PipelineConfig config) {
        if (msg == null) {
            return null;
        }

        MessageData defaultValue = isWarning ? config.getDefaultWarning() : config.getDefaultError();
        MessageData result = isWarning ? msg : MessageData.getFirstNonNull(null, msg, defaultValue);
        return result != null ? result.fixCallObj(this) : null;
    }

    @Specialization
    public Object bypassRNull(RNull x) {
        if (isRNullBypassed) {
            if (nullMsg != null) {
                handleArgumentWarning(x, nullMsg.getCallObj(), nullMsg.getMessage(), nullMsg.getMessageArgs());
            }
            return nullMapFn.map(x);
        } else if (directFindFirstNodeWithDefault != null) {
            return directFindFirstNodeWithDefault.execute(x);
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
        } else if (directFindFirstNodeWithDefault != null) {
            return directFindFirstNodeWithDefault.execute(x);
        } else if (missingMsg == null) {
            // go to the pipeline
            return handleOthers(x);
        } else {
            handleArgumentError(x, missingMsg.getCallObj(), missingMsg.getMessage(), missingMsg.getMessageArgs());
            return x;
        }
    }

    @Specialization(guards = "isNotHandled(x)")
    public Object handleOthers(Object x) {
        try {
            return noHead ? x : wrappedHead.execute(x);
        } catch (PipelineReturnException ret) {
            return ret.getResult();
        }
    }

    protected boolean isNotHandled(Object x) {
        return x != RNull.instance && x != RMissing.instance;
    }

    public static CastNode create(PipelineConfig pipelineConfig, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNodeWithDefault) {
        return BypassNodeGen.create(pipelineConfig, wrappedHead, mapperFactory, directFindFirstNodeWithDefault, null);
    }

    public abstract static class BypassIntegerNode extends BypassNode {
        public BypassIntegerNode(PipelineConfig pcb, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNode,
                        CastNode afterFindFirst) {
            super(pcb, wrappedHead, mapperFactory, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        protected Object bypassInteger(int x) {
            return executeAfterFindFirst(x);
        }

        @Override
        protected boolean isNotHandled(Object x) {
            return super.isNotHandled(x) && !(x instanceof Integer);
        }
    }

    public abstract static class BypassDoubleNode extends BypassNode {
        public BypassDoubleNode(PipelineConfig pcb, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNode,
                        CastNode afterFindFirst) {
            super(pcb, wrappedHead, mapperFactory, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        public Object bypassDouble(double x) {
            return executeAfterFindFirst(x);
        }

        @Override
        protected boolean isNotHandled(Object x) {
            return super.isNotHandled(x) && !(x instanceof Integer);
        }
    }

    public abstract static class BypassStringNode extends BypassNode {
        public BypassStringNode(PipelineConfig pcb, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNode,
                        CastNode afterFindFirst) {
            super(pcb, wrappedHead, mapperFactory, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        public Object bypassString(String x) {
            return executeAfterFindFirst(x);
        }

        @Override
        protected boolean isNotHandled(Object x) {
            return super.isNotHandled(x) && !(x instanceof String);
        }
    }

    public abstract static class BypassLogicalMapToBooleanNode extends BypassNode {
        public BypassLogicalMapToBooleanNode(PipelineConfig pcb, CastNode wrappedHead, ArgumentMapperFactory mapperFactory, CastNode directFindFirstNode,
                        CastNode afterFindFirst) {
            super(pcb, wrappedHead, mapperFactory, directFindFirstNode, afterFindFirst);
        }

        @Specialization
        public boolean bypassLogical(byte x) {
            return RRuntime.fromLogical(x);
        }

        @Override
        protected boolean isNotHandled(Object x) {
            return super.isNotHandled(x) && !(x instanceof Byte);
        }
    }
}
