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
import com.oracle.truffle.r.nodes.builtin.ArgumentMapper;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.DefaultError;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.PipelineConfigBuilder;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class BypassNode extends CastNode {

    private final boolean isRNullBypassed;
    private final DefaultError nullMsg;
    private final ArgumentMapper nullMapFn;

    private final boolean isRMissingBypassed;
    private final DefaultError missingMsg;
    private final ArgumentMapper missingMapFn;
    private final boolean noHead;

    @Child private CastNode wrappedHead;
    @Child private CastNode directFindFirstNode;
    private final boolean useDirectFindFirstNode;

    protected BypassNode(PipelineConfigBuilder pcb, CastNode wrappedHead) {
        this.nullMapFn = pcb.getNullMapper();
        this.isRNullBypassed = this.nullMapFn != null;
        this.nullMsg = pcb.getNullMessage() == null ? null : pcb.getNullMessage().fixCallObj(this);

        this.missingMapFn = pcb.getMissingMapper();
        this.isRMissingBypassed = this.missingMapFn != null;
        this.missingMsg = pcb.getMissingMessage() == null ? null : pcb.getMissingMessage().fixCallObj(this);

        this.wrappedHead = wrappedHead;
        this.noHead = wrappedHead == null;

        assert this.nullMsg != null || this.isRNullBypassed;
        assert this.missingMsg != null || this.isRMissingBypassed;

        this.directFindFirstNode = !isRNullBypassed || !isRMissingBypassed ? createDirectFindFirstNode(wrappedHead) : null;
        this.useDirectFindFirstNode = directFindFirstNode != null;
    }

    public static CastNode create(PipelineConfigBuilder pcb, CastNode wrappedHead) {
        return BypassNodeGen.create(pcb, wrappedHead);
    }

    public CastNode getWrappedHead() {
        return wrappedHead;
    }

    public ArgumentMapper getNullMapper() {
        return nullMapFn;
    }

    public ArgumentMapper getMissingMapper() {
        return missingMapFn;
    }

    @Specialization
    public Object bypassRNull(RNull x) {
        if (isRNullBypassed) {
            if (nullMsg != null) {
                handleArgumentWarning(x, nullMsg.callObj, nullMsg.message, nullMsg.args);
            }
            return nullMapFn.map(x);
        } else if (useDirectFindFirstNode) {
            return directFindFirstNode.execute(x);
        } else {
            handleArgumentError(x, nullMsg.callObj, nullMsg.message, nullMsg.args);
            return x;
        }
    }

    @Specialization
    public Object bypassRMissing(RMissing x) {
        if (isRMissingBypassed) {
            if (missingMsg != null) {
                handleArgumentWarning(x, missingMsg.callObj, missingMsg.message, missingMsg.args);
            }
            return missingMapFn.map(x);
        } else if (useDirectFindFirstNode) {
            return directFindFirstNode.execute(x);
        } else {
            handleArgumentError(x, missingMsg.callObj, missingMsg.message, missingMsg.args);
            return x;
        }
    }

    @Fallback
    public Object handleOthers(Object x) {
        return noHead ? x : wrappedHead.execute(x);
    }

    static CastNode createDirectFindFirstNode(CastNode wrappedHead) {
        ChainedCastNode parentFfh = null;
        ChainedCastNode ffh = null;

        if (wrappedHead != null) {
            CastNode cn = wrappedHead;
            while (cn instanceof ChainedCastNode) {
                ChainedCastNode chcn = (ChainedCastNode) cn;
                if (chcn.getSecondCast() instanceof FindFirstNode) {
                    FindFirstNode ffn = (FindFirstNode) chcn.getSecondCast();
                    if (ffn.getDefaultValue() != null) {
                        ffh = chcn;
                    }
                    break;
                }
                parentFfh = chcn;
                cn = chcn.getFirstCast();
            }
        }

        if (ffh == null) {
            return null;
        } else if (parentFfh == null) {
            return ffh.getSecondCastFact().create();
        } else {
            return new ChainedCastNode(ffh.getSecondCastFact(), parentFfh.getSecondCastFact());
        }
    }
}
