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
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * A "fake" {@link VirtualFrame}, to be used by {@code REngine}.eval only!
 */
public abstract class VirtualEvalFrame extends SubstituteVirtualFrame implements VirtualFrame, MaterializedFrame {

    @CompilationFinal(dimensions = 1) protected final Object[] arguments;

    private VirtualEvalFrame(MaterializedFrame originalFrame, Object[] arguments) {
        super(originalFrame);
        this.arguments = arguments;
    }

    @Override
    public abstract MaterializedFrame getOriginalFrame();

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public MaterializedFrame materialize() {
        return this;
    }

    private static final class Substitute1 extends VirtualEvalFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute1(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class Substitute2 extends VirtualEvalFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute2(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class SubstituteGeneric extends VirtualEvalFrame {

        protected SubstituteGeneric(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return originalFrame;
        }
    }

    public static VirtualEvalFrame create(MaterializedFrame originalFrame, RFunction function, RCaller call) {
        Object[] arguments = Arrays.copyOf(originalFrame.getArguments(), originalFrame.getArguments().length);
        arguments[RArguments.INDEX_IS_IRREGULAR] = true;
        arguments[RArguments.INDEX_FUNCTION] = function;
        arguments[RArguments.INDEX_CALL] = call;
        MaterializedFrame unwrappedFrame = originalFrame instanceof SubstituteVirtualFrame ? ((SubstituteVirtualFrame) originalFrame).getOriginalFrame() : originalFrame;
        @SuppressWarnings("unchecked")
        Class<MaterializedFrame> clazz = (Class<MaterializedFrame>) unwrappedFrame.getClass();
        if (Substitute1.frameClass == clazz) {
            return new Substitute1(unwrappedFrame, arguments);
        } else if (Substitute1.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute1.frameClass = clazz;
            return new Substitute1(unwrappedFrame, arguments);
        } else if (Substitute2.frameClass == clazz) {
            return new Substitute2(unwrappedFrame, arguments);
        } else if (Substitute2.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute2.frameClass = clazz;
            return new Substitute2(unwrappedFrame, arguments);
        } else {
            return new SubstituteGeneric(unwrappedFrame, arguments);
        }
    }
}
