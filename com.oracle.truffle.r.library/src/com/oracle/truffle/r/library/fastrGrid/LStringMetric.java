/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/**
 * Returns a list with string's width, ascent and descent all in inches.
 */
public abstract class LStringMetric extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(LStringMetric.class);
        casts.arg(0).mustBe(stringValue()).asStringVector();
    }

    public static LStringMetric create() {
        return LStringMetricNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    public Object execute(RAbstractStringVector text) {
        int len = text.getLength();

        // Needs to be determined if ascent/descent are actually used by anyone without knowing the
        // actual string height
        double[] ascent = new double[len];
        double[] descent = new double[len];
        Arrays.fill(ascent, 0.);
        Arrays.fill(descent, 0.);

        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        double[] width = new double[len];
        for (int i = 0; i < text.getLength(); i++) {
            width[i] = GridUtils.getStringWidth(gpar.getDrawingContext(i), dev, text.getDataAt(i));
        }

        return RDataFactory.createList(new Object[]{asVec(ascent), asVec(descent), asVec(width)});
    }

    private static RAbstractDoubleVector asVec(double[] data) {
        return RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
    }
}
