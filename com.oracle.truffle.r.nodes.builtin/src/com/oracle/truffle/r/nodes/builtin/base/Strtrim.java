/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "strtrim", kind = INTERNAL, parameterNames = {"x", "width"}, behavior = PURE)
public abstract class Strtrim extends RBuiltinNode {

    private RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    @Specialization
    protected RStringVector srtrim(RAbstractStringVector x, RAbstractIntVector width) {
        int len = x.getLength();
        int nw = width.getLength();
        if (nw == 0 || nw < len && (len % nw != 0)) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "width");
        }
        for (int i = 0; i < nw; i++) {
            int widthi = width.getDataAt(i);
            if (widthi == RRuntime.INT_NA || widthi < 0) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "width");
            }
        }
        String[] data = new String[len];
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        for (int i = 0; i < len; i++) {
            String element = x.getDataAt(i);
            if (RRuntime.isNA(element)) {
                data[i] = element;
                complete = RDataFactory.INCOMPLETE_VECTOR;
                continue;
            }
            // TODO multibyte character handling
            int w = width.getDataAt(i % nw);
            if (w > element.length()) {
                data[i] = element;
            } else {
                data[i] = element.substring(0, w);
            }
        }
        RStringVector result = RDataFactory.createStringVector(data, complete);
        result.copyAttributesFrom(attrProfiles, x);
        return result;
    }

    @SuppressWarnings("unused")
    @Fallback
    @TruffleBoundary
    RStringVector strtrim(Object x, Object width) {
        throw RError.error(this, RError.Message.REQUIRES_CHAR_VECTOR, "strtrim()");
    }
}
