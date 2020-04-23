/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.RandomAccessIterator;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@ExportLibrary(VectorDataLibrary.class)
public class RStringSeqVectorData implements RSeq {
    private final int start;
    private final int stride;
    private final String prefix;
    private final String suffix;
    private final int length;

    protected RStringSeqVectorData(String prefix, String suffix, int start, int stride, int length) {
        this.start = start;
        this.stride = stride;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.length = length;
    }

    @Override
    public Object getStartObject() {
        return start;
    }

    @Override
    public Object getStrideObject() {
        return stride;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return start + (getLength() - 1) * stride;
    }

    public int getStride() {
        return stride;
    }

    @ExportMessage
    @Override
    public int getLength() {
        return length;
    }

    public int getIndexFor(String element) {
        if ((prefix.length() > 0 && !element.startsWith(prefix)) || (suffix.length() > 0 && !element.endsWith(suffix))) {
            return -1;
        }
        String c = element.substring(prefix.length(), element.length() - suffix.length());
        try {
            int current = Integer.parseInt(c);
            int first = Math.min(getStart(), getEnd());
            int last = Math.max(getStart(), getEnd());
            if (current < first || current > last) {
                return -1;
            }
            if ((current - getStart()) % getStride() == 0) {
                return (current - getStart()) / getStride();
            }
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    // VectorDataLibrary:

    @SuppressWarnings("static-method")
    @ExportMessage
    public NACheck getNACheck() {
        return NACheck.getDisabled();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public final RType getType() {
        return RType.Character;
    }

    @ExportMessage
    public RStringArrayVectorData materialize() {
        return new RStringArrayVectorData(getStringDataCopy(), true);
    }

    @ExportMessage
    public RStringSeqVectorData copy(@SuppressWarnings("unused") boolean deep) {
        return new RStringSeqVectorData(prefix, suffix, start, stride, length);
    }

    @ExportMessage
    public boolean isComplete() {
        return true;
    }

    @ExportMessage
    public String[] getStringDataCopy() {
        String[] result = new String[length];
        for (int i = 0; i < result.length; i++) {
            result[i] = getStringImpl(i);
        }
        return result;
    }

    // Read access to the elements:

    @ExportMessage
    public SeqIterator iterator(@Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        SeqIterator it = new SeqIterator(null, length);
        it.initLoopConditionProfile(loopProfile);
        return it;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean nextImpl(SeqIterator it, boolean loopCondition,
                    @Shared("SeqItLoopProfile") @Cached("createCountingProfile()") LoopConditionProfile loopProfile) {
        return it.next(loopCondition, loopProfile);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public void nextWithWrap(SeqIterator it,
                    @Cached("createBinaryProfile()") ConditionProfile wrapProfile) {
        it.nextWithWrap(wrapProfile);
    }

    @ExportMessage
    public RandomAccessIterator randomAccessIterator() {
        return new RandomAccessIterator(null);
    }

    @ExportMessage
    public String getStringAt(int index) {
        return getStringImpl(index);
    }

    @ExportMessage
    public String getNextString(SeqIterator it) {
        return getStringImpl(it.getIndex());
    }

    @ExportMessage
    public String getString(@SuppressWarnings("unused") RandomAccessIterator it, int index) {
        return getStringImpl(index);
    }

    // Utility methods:

    private String getStringImpl(int index) {
        assert index >= 0 && index < getLength();
        return prefix + (start + stride * index) + suffix;
    }
}
