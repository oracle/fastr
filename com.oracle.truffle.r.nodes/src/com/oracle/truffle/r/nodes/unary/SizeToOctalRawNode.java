/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2000-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

@ImportStatic(RRuntime.class)
public abstract class SizeToOctalRawNode extends UnaryNode {

    @Child private VectorFactory factory = VectorFactory.create();
    @Child private VectorAccess resultAccess = VectorAccess.createNew(RType.Raw);

    public abstract RRawVector execute(Object size);

    @Specialization(guards = "!isNA(size)")
    protected RRawVector doInt(int size) {
        if (size < 0) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "size must be finite, >= 0 and < 2^33");
        }
        return toOctal(size);
    }

    // Transcribed from ".../utils/src/stubs.c"
    @Specialization(guards = "!isNAorNaN(size)")
    protected RRawVector doDouble(double size) {
        if (!RRuntime.isFinite(size) || size < 0 || size >= 8589934592d /* 2^33 */) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "size must be finite, >= 0 and < 2^33");
        }
        return toOctal((long) size);
    }

    private RRawVector toOctal(long size) {
        RRawVector ans = factory.createRawVector(11);
        try (RandomIterator iter = resultAccess.randomAccess(ans)) {
            long s = size;
            for (int i = 0; i < 11; i++) {
                resultAccess.setRaw(iter, 10 - i, (byte) (48.0 + (s % 8)));
                s /= 8;
            }
        }
        return ans;
    }

    @Fallback
    protected RRawVector doNull(@SuppressWarnings("unused") Object obj) {
        return RDataFactory.createRawVector(11);
    }

    public static SizeToOctalRawNode create() {
        return SizeToOctalRawNodeGen.create();
    }
}
