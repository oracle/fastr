/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.runtime.parsermetadata;

import java.util.Objects;

import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;

public final class LocalVariable {
    private final String name;
    private final FrameSlotKind slotKind;
    private final int frameIndex;

    public LocalVariable(String name, FrameSlotKind slotKind, int frameIndex) {
        assert FrameIndex.isInitializedIndex(frameIndex);
        this.name = name;
        this.slotKind = slotKind;
        this.frameIndex = frameIndex;
    }

    public String getName() {
        return name;
    }

    public FrameSlotKind getSlotKind() {
        return slotKind;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LocalVariable that = (LocalVariable) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "LocalVariable{'" + name + "', " + frameIndex + ", " + slotKind + '}';
    }
}
