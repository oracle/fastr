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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.NodeFields;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@NodeFields({@NodeField(name = "emptyError", type = RError.Message.class), @NodeField(name = "argumentName", type = String.class)})
public abstract class FirstStringNode extends CastNode {

    protected abstract RError.Message getEmptyError();

    protected abstract String getArgumentName();

    private final ConditionProfile lengthOneProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();

    public final String executeString(Object argument) {
        return (String) execute(argument);
    }

    @Specialization
    protected String firstScalar(String argument) {
        return argument;
    }

    @Specialization(replaces = "firstScalar")
    protected String firstVector(RAbstractStringVector argument) {
        if (!lengthOneProfile.profile(argument.getLength() == 1)) {
            errorProfile.enter();
            throw RError.error(RError.SHOW_CALLER, getEmptyError(), getArgumentName());
        }
        return argument.getDataAt(0);
    }

    @Fallback
    protected String firstVectorFallback(@SuppressWarnings("unused") Object argument) {
        throw RError.error(RError.SHOW_CALLER, getEmptyError(), getArgumentName());
    }

    public static FirstStringNode createWithError(RError.Message emptyError, String argumentName) {
        return FirstStringNodeGen.create(emptyError, argumentName);
    }
}
