/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;

public abstract class RBounded {

    public final int getLength() {
        return internalGetLength();
    }

    public boolean isInBounds(int position) {
        return position >= 1 && position <= getLength();
    }

    protected abstract int internalGetLength();

    public final void verifyDimensions(VirtualFrame frame, int[] newDimensions, SourceSection sourceSection) {
        int length = 1;
        for (int i = 0; i < newDimensions.length; i++) {
            if (RRuntime.isNA(newDimensions[i])) {
                throw RError.error(frame, sourceSection, RError.Message.DIMS_CONTAIN_NA);
            } else if (newDimensions[i] < 0) {
                throw RError.error(frame, sourceSection, RError.Message.DIMS_CONTAIN_NEGATIVE_VALUES);
            }
            length *= newDimensions[i];
        }
        if (length != getLength()) {
            throw RError.error(frame, sourceSection, RError.Message.DIMS_DONT_MATCH_LENGTH, length, getLength());
        }
    }

}
