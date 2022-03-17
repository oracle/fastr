/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Description of different internal frame slots used by FastR. This enum is used as an identifier,
 * so that these internal frame slots have non-string names.
 * 
 * IMPORTANT NOTE: this class deliberately does not override {@code equals} and {@code hashCode} and
 * uses reference equality. New instances should not be created dynamically but only as
 * "singletons".
 */
public final class RFrameSlot {
    private static final ArrayList<RFrameSlot> defaultTempIdentifiers = new ArrayList<>();

    private final String name;
    private final boolean multiSlot;
    private final int frameIdx;

    private RFrameSlot(String name, boolean multiSlot) {
        this(name, multiSlot, FrameIndex.UNITIALIZED_INDEX);
    }

    private RFrameSlot(String name, boolean multiSlot, int frameIdx) {
        this.name = name;
        this.multiSlot = multiSlot;
        this.frameIdx = frameIdx;
    }

    @Override
    public String toString() {
        return name == null ? "TempFrameSlot" : name;
    }

    public int getFrameIdx() {
        return frameIdx;
    }

    public boolean isTemp() {
        return name == null;
    }

    public boolean isMultiSlot() {
        return multiSlot;
    }

    public static RFrameSlot getTemp(int idx) {
        if (idx >= defaultTempIdentifiers.size()) {
            for (int i = defaultTempIdentifiers.size(); i <= idx; i++) {
                defaultTempIdentifiers.add(RFrameSlot.createTemp("TempFrameSlot" + i, true));
            }
        }
        return defaultTempIdentifiers.get(idx);
    }

    private static RFrameSlot createTemp(String name, boolean multiSlot) {
        return new RFrameSlot(name, multiSlot);
    }

    /**
     * This frame slot is used to store expressions installed as function exit handlers via on.exit.
     * It contains an {@link ArrayList} with {@link RNode} elements.
     */
    public static final RFrameSlot OnExit = new RFrameSlot("OnExit", false);
    /**
     * This frame slot is used to track result visibility. It can contain one of three values:
     * <ul>
     * <li>{@link Boolean#TRUE} if the result is currently visible</li>
     * <li>{@link Boolean#FALSE} if the result is currently not visible</li>
     * <li>{@code null} if the visibility was not set yet</li>
     * </ul>
     *
     * Whenever an {@code RBuiltinNode} or R function is called via {@code RCallNode}, the resulting
     * visibility is stored in the current frame. At the end of a {@code FunctionDefinitionNode},
     * the current state is stored into {@link RCaller#setVisibility(boolean)} if it is non-
     * {@code null}. After each call site, the value of {@link RCaller#getVisibility()} is extracted
     * and stored into the frame slot. Note: the {@link RCaller} is passed as an argument to the
     * callee frame. The callee can "return" the visibility to the caller via setting it in the
     * given {@link RCaller} instance.
     */
    public static final RFrameSlot Visibility = new RFrameSlot("Visibility", false);
    /**
     * Used to save the handler stack in frames that modify it.
     */
    public static final RFrameSlot HandlerStack = new RFrameSlot("HandlerStack", false);
    /**
     * Used to save the restart stack in frames that modify it.
     */
    public static final RFrameSlot RestartStack = new RFrameSlot("RestartStack", false);

    public static final RFrameSlot ExplicitCallArgs = new RFrameSlot("RExplicitCall-argsIdentifier", true);

    public static final RFrameSlot FunctionEvalNodeArgsIdentifier = RFrameSlot.createTemp("FunctionEvalCallNode-argsIdentifier", true);

    public static final RFrameSlot FunctionEvalNodeFunIdentifier = RFrameSlot.createTemp("FunctionEvalCallNode-funIdentifier", true);

    public static RFrameSlot[] values() {
        return new RFrameSlot[]{OnExit, Visibility, HandlerStack, RestartStack};
    }
}
