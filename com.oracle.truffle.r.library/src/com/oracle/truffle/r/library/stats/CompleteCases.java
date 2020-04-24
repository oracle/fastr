/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.stats;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

// Transcribed from GnuR, library/stats/src/complete_cases.c

public final class CompleteCases extends RExternalBuiltinNode {

    static {
        Casts.noCasts(CompleteCases.class);
    }

    private RError invalidType(Object entry) {
        throw error(RError.Message.INVALID_TYPE_ARGUMENT, ((RBaseObject) entry).getRType().getName());
    }

    private RError lengthMismatch() {
        throw error(RError.Message.NOT_ALL_SAME_LENGTH);
    }

    private int checkAbstractVectorLength(int len, Object obj) {
        Object entry = RRuntime.asAbstractVector(obj);
        if (entry instanceof RAbstractVector) {
            RAbstractVector vector = (RAbstractVector) entry;
            int entryLength = vector.isMatrix() ? vector.getDimensions()[0] : vector.getLength();
            if (len < 0) {
                return entryLength;
            } else if (len != entryLength) {
                throw lengthMismatch();
            }
        } else {
            throw invalidType(entry);
        }
        return len;
    }

    @Override
    @TruffleBoundary
    public Object call(RArgsValuesAndNames args) {
        int len = -1;
        for (int i = 0; i < args.getLength(); i++) {
            Object arg = args.getArgument(i);
            if (arg instanceof RNull) {
                // nothing to do
            } else if ((arg instanceof RPairList && !((RPairList) arg).isLanguage())) {
                for (Object t = ((RPairList) arg).car(); t != RNull.instance; t = ((RPairList) t).cdr()) {
                    len = checkAbstractVectorLength(len, ((RPairList) t).car());
                }
            } else if (arg instanceof RList) {
                RList list = (RList) arg;
                if (list.getLength() > 0) {
                    for (int entry = 0; entry < list.getLength(); entry++) {
                        len = checkAbstractVectorLength(len, list.getDataAt(entry));
                    }
                } else if (list.getRowNames() != RNull.instance) {
                    /* 0-column data frames are a special case */
                    int entryLength = ((RAbstractVector) list.getRowNames()).getLength();
                    if (len < 0) {
                        len = entryLength;
                    } else if (len != entryLength) {
                        throw lengthMismatch();
                    }
                }
            } else {
                len = checkAbstractVectorLength(len, arg);
            }
        }

        if (len < 0) {
            throw error(RError.Message.NO_INPUT_NUMBER_OF_CASES);
        }

        byte[] result = new byte[len];
        Arrays.fill(result, RRuntime.LOGICAL_TRUE);

        for (int i = 0; i < args.getLength(); i++) {
            Object arg = args.getArgument(i);
            if ((arg instanceof RPairList && !((RPairList) arg).isLanguage())) {
                for (Object t = ((RPairList) arg).car(); t != RNull.instance; t = ((RPairList) t).cdr()) {
                    Object entry = ((RPairList) t).car();
                    iterateAbstractVectorContents(len, result, entry);
                }
            } else if (arg instanceof RList) {
                RList list = (RList) arg;
                for (int entry = 0; entry < list.getLength(); entry++) {
                    iterateAbstractVectorContents(len, result, list.getDataAt(entry));
                }
            } else {
                iterateAbstractVectorContents(len, result, arg);
            }
        }

        return RDataFactory.createLogicalVector(result, true);
    }

    private void iterateAbstractVectorContents(int len, byte[] result, Object obj) {
        Object entry = RRuntime.asAbstractVector(obj);
        if (entry instanceof RIntVector) {
            RIntVector v = (RIntVector) entry;
            for (int e = 0; e < v.getLength(); e++) {
                if (RRuntime.isNA(v.getDataAt(e))) {
                    result[e % len] = RRuntime.LOGICAL_FALSE;
                }
            }
        } else if (entry instanceof RLogicalVector) {
            RLogicalVector v = (RLogicalVector) entry;
            for (int e = 0; e < v.getLength(); e++) {
                if (RRuntime.isNA(v.getDataAt(e))) {
                    result[e % len] = RRuntime.LOGICAL_FALSE;
                }
            }
        } else if (entry instanceof RDoubleVector) {
            RDoubleVector v = (RDoubleVector) entry;
            for (int e = 0; e < v.getLength(); e++) {
                if (Double.isNaN(v.getDataAt(e))) {
                    result[e % len] = RRuntime.LOGICAL_FALSE;
                }
            }
        } else if (entry instanceof RComplexVector) {
            RComplexVector v = (RComplexVector) entry;
            for (int e = 0; e < v.getLength(); e++) {
                if (Double.isNaN(v.getDataAt(e).getRealPart()) || Double.isNaN(v.getDataAt(e).getImaginaryPart())) {
                    result[e % len] = RRuntime.LOGICAL_FALSE;
                }
            }
        } else if (entry instanceof RAbstractStringVector) {
            RAbstractStringVector v = (RAbstractStringVector) entry;
            for (int e = 0; e < v.getLength(); e++) {
                if (RRuntime.isNA(v.getDataAt(e))) {
                    result[e % len] = RRuntime.LOGICAL_FALSE;
                }
            }
        } else if (entry == RNull.instance) {
            // ignore NULL values
        } else {
            throw invalidType(entry);
        }
    }
}
