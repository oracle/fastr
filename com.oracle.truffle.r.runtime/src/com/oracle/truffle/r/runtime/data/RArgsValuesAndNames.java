/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.r.runtime.Arguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * A simple wrapper class for passing the ... argument through RArguments
 */
public final class RArgsValuesAndNames extends Arguments<Object> implements RTypedValue {
    /**
     * Default instance for empty "..." ("..." that resolve to contain no expression at runtime).
     * The {@link RMissing#instance} for "...".
     */
    public static final RArgsValuesAndNames EMPTY = new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));

    public RArgsValuesAndNames(Object[] values, ArgumentsSignature signature) {
        super(values, signature);
        assert signature != null && signature.getLength() == values.length : Arrays.toString(values) + " " + signature;
    }

    @Override
    public RType getRType() {
        return RType.Dots;
    }

    @Override
    public int getTypedValueInfo() {
        // RArgsValuesAndNames can get serialized under specific circumstances (ggplot2 does that)
        // and getTypedValueInfo() must be defined for this to work.
        return 0;
    }

    @Override
    public void setTypedValueInfo(int value) {
        throw RInternalError.shouldNotReachHere();
    }

    public RPairList toPairlist() {
        RPairList head = null;
        ArgumentsSignature signature = getSignature();
        assert signature.getLength() == getLength();
        for (int i = 0; i < getLength(); i++) {
            String name = signature.getName(i);
            RPairList cur = RDataFactory.createPairList(getArgument(i), RNull.instance, name != null ? name : RNull.instance, SEXPTYPE.DOTSXP);

            if (head == null) {
                head = cur;
            } else {
                head.appendToEnd(cur);
            }
        }
        if (head != null) {
            return head;
        }
        return RDataFactory.createPairList(RNull.instance);
    }
}
