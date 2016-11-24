/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;

public abstract class CastStringBaseNode extends CastBaseNode {

    @Child private ToStringNode toString = ToStringNodeGen.create();

    protected CastStringBaseNode(boolean preserveNames, boolean preserveDimensions, boolean preserveAttributes) {
        super(preserveNames, preserveDimensions, preserveAttributes);
    }

    @Override
    protected final RType getTargetType() {
        return RType.Character;
    }

    @Specialization
    protected String doString(String value) {
        return value;
    }

    protected String toString(Object value) {
        return toString.executeString(value, false, ToStringNode.DEFAULT_SEPARATOR);
    }

    @Specialization
    protected String doInteger(int value) {
        return toString(value);
    }

    @Specialization
    protected String doDouble(double value) {
        return toString(value);
    }

    @Specialization
    protected String doLogical(byte value) {
        return toString(value);
    }

    @Specialization
    protected String doRaw(RComplex value) {
        return toString(value);
    }

    @Specialization
    protected String doRaw(RRaw value) {
        return toString(value);
    }

    @Specialization
    protected RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    protected RMissing doMissing(RMissing missing) {
        return missing;
    }
}
