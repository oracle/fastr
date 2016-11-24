/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class CastStringNode extends CastStringBaseNode {

    protected CastStringNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    public abstract Object executeString(int o);

    public abstract Object executeString(double o);

    public abstract Object executeString(byte o);

    public abstract Object executeString(Object o);

    private RStringVector vectorCopy(RAbstractContainer operand, String[] data) {
        RStringVector ret = RDataFactory.createStringVector(data, operand.isComplete(), getPreservedDimensions(operand), getPreservedNames(operand));
        preserveDimensionNames(operand, ret);
        if (preserveAttributes()) {
            ret.copyRegAttributesFrom(operand);
        }
        return ret;
    }

    @Specialization
    protected RStringVector doStringVector(RStringVector vector) {
        return vector;
    }

    @Specialization
    protected RStringVector doAbstractContainer(RAbstractContainer operand,
                    @Cached("createBinaryProfile()") ConditionProfile isLanguageProfile) {
        String[] sdata = new String[operand.getLength()];
        // conversions to character will not introduce new NAs
        for (int i = 0; i < operand.getLength(); i++) {
            Object o = operand.getDataAtAsObject(i);
            if (isLanguageProfile.profile(o instanceof RLanguage)) {
                sdata[i] = RDeparse.deparse(o);
            } else {
                sdata[i] = toString(o);
            }
        }
        return vectorCopy(operand, sdata);
    }

    @Specialization
    protected String doRSymbol(RSymbol s) {
        return s.getName();
    }

    public static CastStringNode create() {
        return CastStringNodeGen.create(true, true, true);
    }

    public static CastStringNode createNonPreserving() {
        return CastStringNodeGen.create(false, false, false);
    }
}
