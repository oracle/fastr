/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import java.util.ArrayList;

import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Description of different internal frame slots used by FastR. This enum is used as an identifier,
 * so that these internal frame slots have non-string names.
 */
public final class RFrameSlot {
    private final String name;
    private final boolean multiSlot;

    private RFrameSlot(String name, boolean multiSlot) {
        this.name = name;
        this.multiSlot = multiSlot;
    }

    @Override
    public String toString() {
        return name == null ? "TempFrameSlot" : name;
    }

    public boolean isTemp() {
        return name == null;
    }

    public boolean isMultiSlot() {
        return multiSlot;
    }

    public static RFrameSlot createTemp(boolean multiSlot) {
        return new RFrameSlot(null, multiSlot);
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
     * Whenever an {@RBuiltinNode} is called via {@code RCallNode}, the resulting visibility is
     * stored in the current frame. At the end of a {@code FunctionDefinitionNode}, the current
     * state is stored into {@link RCaller#setVisibility(boolean)} if it is non-{@code null}. After
     * each call site, the value of {@link RCaller#getVisibility()} is extracted and stored into the
     * frame slot.
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

    public static RFrameSlot[] values() {
        return new RFrameSlot[]{OnExit, Visibility, HandlerStack, RestartStack};
    }
}
