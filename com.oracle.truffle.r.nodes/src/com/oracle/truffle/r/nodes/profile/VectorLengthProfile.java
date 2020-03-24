/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.profile;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class VectorLengthProfile {

    private static final VectorLengthProfile UNCACHED = new Disabled();

    public static VectorLengthProfile create() {
        return new Enabled();
    }

    public static VectorLengthProfile getUncached() {
        return UNCACHED;
    }

    public abstract int profile(int length);

    /**
     * Any negative number indicates that no concrete length value is cached (profile is
     * uninitialized or generic).
     */
    public abstract int getCachedLength();

    private static final class Disabled extends VectorLengthProfile {
        @Override
        public int profile(int length) {
            return length;
        }

        @Override
        public int getCachedLength() {
            return -1;
        }
    }

    private static final class Enabled extends VectorLengthProfile {
        private static final int MAX_PROFILED_LENGTH = 4;
        private static final int UNINITIALIZED_LENGTH = -2;
        private static final int GENERIC_LENGTH = -1;

        @CompilationFinal private int cachedLength;

        private Enabled() {
            cachedLength = UNINITIALIZED_LENGTH;
        }

        @Override
        public int profile(int length) {
            assert length >= 0;
            if (cachedLength == GENERIC_LENGTH) {
                return length;
            } else if (cachedLength == UNINITIALIZED_LENGTH) {
                // check the uninitialized case first - this way an uninitialized profile will deopt
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedLength = length <= MAX_PROFILED_LENGTH ? length : GENERIC_LENGTH;
            } else if (cachedLength == length) {
                return cachedLength;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedLength = GENERIC_LENGTH;
            }
            return length;
        }

        @Override
        public int getCachedLength() {
            return cachedLength;
        }

        @Override
        public String toString() {
            String length;
            if (cachedLength == GENERIC_LENGTH) {
                length = "generic";
            } else if (cachedLength == UNINITIALIZED_LENGTH) {
                length = "uninitialized";
            } else {
                length = "==" + cachedLength;
            }
            return String.format("%s(%s)", VectorLengthProfile.class.getSimpleName(), length);
        }
    }
}
