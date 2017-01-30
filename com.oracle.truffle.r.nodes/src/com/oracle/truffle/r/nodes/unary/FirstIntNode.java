/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@NodeFields({@NodeField(name = "emptyError", type = RError.Message.class), @NodeField(name = "sizeWarning", type = RError.Message.class), @NodeField(name = "argumentName", type = String.class),
                @NodeField(name = "defaultValue", type = int.class)})
public abstract class FirstIntNode extends CastNode {

    protected abstract RError.Message getEmptyError();

    protected abstract RError.Message getSizeWarning();

    protected abstract String getArgumentName();

    protected abstract int getDefaultValue();

    public abstract int executeInt(Object value);

    private final ConditionProfile lengthOneProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    @Specialization
    protected int firstScalar(int argument) {
        return argument;
    }

    @Specialization(replaces = "firstScalar")
    protected int firstVector(RAbstractIntVector argument) {
        if (!lengthOneProfile.profile(argument.getLength() == 1)) {
            if (getSizeWarning() != null) {
                RError.warning(this, getSizeWarning(), getArgumentName());
                if (argument.getLength() == 0) {
                    return getDefaultValue();
                }
            } else if (getEmptyError() != null && argument.getLength() == 0) {
                errorProfile.enter();
                throw RError.error(this, getEmptyError(), getArgumentName());
            }
        }
        return argument.getDataAt(0);
    }

    public static FirstIntNode createWithWarning(RError.Message sizeWarning, String argumentName, int defaultValue) {
        return FirstIntNodeGen.create(null, sizeWarning, argumentName, defaultValue);
    }

    public static FirstIntNode createWithError(RError.Message emptyError, String argumentName) {
        return FirstIntNodeGen.create(emptyError, null, argumentName, 0);
    }
}
