/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RTypesGen;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.RScope;
import com.oracle.truffle.r.runtime.env.frame.REnvEmptyFrameAccess;
import com.oracle.truffle.r.runtime.env.frame.REnvTruffleFrameAccess;
import com.oracle.truffle.r.runtime.nodes.instrumentation.RNodeWrapper;

@TypeSystemReference(RTypes.class)
@ExportLibrary(NodeLibrary.class)
public abstract class RNode extends RBaseNodeWithWarnings implements RInstrumentableNode {

    @Override
    public boolean isInstrumentable() {
        return (this instanceof RSyntaxElement && ((RSyntaxElement) this).getLazySourceSection() != null) || getSourceSection() != null;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return RContext.getRRuntimeASTAccess().isTaggedWith(this, tag);
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new RNodeWrapper(this, probe);
    }

    /**
     * Normal execute function that is called when the return value, but not its visibility is
     * needed.
     */
    public abstract Object execute(VirtualFrame frame);

    /**
     * This function is called when the result is not needed. Its name does not start with "execute"
     * so that the DSL does not treat it like an execute function.
     */
    public void voidExecute(VirtualFrame frame) {
        execute(frame);
    }

    /**
     * This function is called when both the result and the result's visibility are needed. Its name
     * does not start with "execute" so that the DSL does not treat it like an execute function.
     */
    public Object visibleExecute(VirtualFrame frame) {
        Object result = execute(frame);
        if (CompilerDirectives.inInterpreter() && result == null) {
            throw RInternalError.shouldNotReachHere("null result in " + this.getClass().getSimpleName());
        }
        return result;
    }

    /*
     * Execute functions with primitive return types:
     */

    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectInteger(execute(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectDouble(execute(frame));
    }

    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return RTypesGen.expectByte(execute(frame));
    }

    /*
     * NodeLibrary implementation:
     */

    @ExportMessage
    boolean hasScope(Frame frame) {
        if (isInstrumentable()) {
            REnvironment env = getEnv(frame);
            if (env == null) {
                Object[] arguments = frame.getArguments();
                if (arguments.length == 1 && arguments[0] instanceof MaterializedFrame) {
                    MaterializedFrame unwrapped = (MaterializedFrame) arguments[0];
                    if (RArguments.isRFrame(unwrapped)) {
                        env = getEnv(unwrapped);
                    }
                }
            }
            if (env != REnvironment.globalEnv()) {
                return true;
            }
        }
        return false;
    }

    @ExportMessage
    public Object getScope(Frame requestedFrame, boolean nodeEnter) throws UnsupportedMessageException {
        if (requestedFrame == null) {
            // Historically we ignored this since all variables are created dynamically in R, but
            // with the new API we can at least provide the parents: global scope and the loaded
            // packages. (TODO)
            return new RScope(null, new REnvEmptyFrameAccess(), getRootNode());
        }
        REnvironment env = getEnv(requestedFrame);
        Frame frame = requestedFrame;
        if (env == null) {
            Object[] arguments = requestedFrame.getArguments();
            if (arguments.length == 1 && arguments[0] instanceof MaterializedFrame) {
                MaterializedFrame unwrapped = (MaterializedFrame) arguments[0];
                if (RArguments.isRFrame(unwrapped)) {
                    env = getEnv(unwrapped);
                    frame = unwrapped;
                }
            }
        }

        if (env == REnvironment.globalEnv()) {
            throw UnsupportedMessageException.create();
        }
        if (env != null) {
            return new RScope(env, env.getFrameAccess(), getRootNode());
        }

        MaterializedFrame mFrame = frame.materialize();
        return new RScope(null, new REnvTruffleFrameAccess(mFrame), getRootNode());
    }

    private static REnvironment getEnv(Frame frame) {
        if (RArguments.isRFrame(frame)) {
            return REnvironment.frameToEnvironment(frame.materialize());
        }
        return null;
    }
}
