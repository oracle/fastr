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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.data.RPromise;

/**
 * The purpose of this class is to maintain correct stack depth while evaluating promises, while
 * recording (in {@code originalFrame} the frame associated with the promise, because there are
 * situations, e.g. condition handling, where access is needed to the original frame depth
 * independently from the "logical" frame depth.
 *
 * At the beginning of (non-trivial) promise evaluation an instance of this class is created with
 * the current frame depth and used in place of the promise frame. Therefore when a new frame is
 * created {@link RArguments#getDepth(Frame)} will return the correct "logical" value. As more
 * frames are pushed on the stack, a reference to the {@link PromiseEvalFrame} is recorded in
 * {@link RArguments}, so it is always possible to determine if a promise is being evaluated. To
 * support the {@code sys.xxx} builtins that depend on frame depth the original {@link RPromise}
 * value is also recorded in {@link #promise}.
 *
 * N.B. {@link RArguments#getDepth} called on a {@link PromiseEvalFrame} returns the depth at the
 * time the {@link PromiseEvalFrame} was created. {@link RArguments#getDepth} called on
 * {@link #getOriginalFrame()} returns the depth of the frame associated with {@link #promise}.
 *
 * N.B. Also, it is important that the {@link PromiseEvalFrame} appears to properly mimic the
 * original promise frame, in that the {@link RArguments} array be the same.
 *
 *
 */
public final class PromiseEvalFrame extends AbstractVirtualEvalFrame {

    /**
     * The promise being evaluated.
     */
    private final RPromise promise;

    private PromiseEvalFrame(Frame topFrame, MaterializedFrame originalFrame, RPromise promise) {
        super(originalFrame, RArguments.getFunction(originalFrame), RArguments.getCall(originalFrame), RArguments.getDepth(topFrame));
        this.promise = promise;
    }

    public static PromiseEvalFrame create(Frame topFrame, MaterializedFrame promiseFrame, RPromise promise) {
        PromiseEvalFrame result = new PromiseEvalFrame(topFrame, promiseFrame instanceof PromiseEvalFrame ? ((PromiseEvalFrame) promiseFrame).getOriginalFrame() : promiseFrame, promise);
        result.arguments[RArguments.INDEX_PROMISE_FRAME] = result;
        return result;
    }

    public int getPromiseFrameDepth() {
        return RArguments.getDepth(getOriginalFrame());
    }

    public RPromise getPromise() {
        return promise;
    }

}
