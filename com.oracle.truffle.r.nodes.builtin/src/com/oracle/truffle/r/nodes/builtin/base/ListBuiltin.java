/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "list", kind = PRIMITIVE, parameterNames = {"..."})
// TODO Is it really worth having all the individual specializations given that we have to have one
// for *every* type
// and the code is essentially equivalent for each one?
public abstract class ListBuiltin extends RBuiltinNode {

    private final ConditionProfile namesNull = ConditionProfile.createBinaryProfile();
    private final BranchProfile shareable = BranchProfile.create();
    private final BranchProfile temporary = BranchProfile.create();

    private RList listArgs(ArgumentsSignature suppliedSignature, Object[] args) {
        controlVisibility();
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            if (value instanceof RShareable) {
                shareable.enter();
                if (FastROptions.NewStateTransition.getBooleanValue()) {
                    if (((RShareable) value).isTemporary()) {
                        temporary.enter();
                        ((RShareable) value).incRefCount();
                    }
                } else {
                    ((RShareable) value).makeShared();
                }
            }
        }
        return RDataFactory.createList(args, argNameVector(suppliedSignature));
    }

    private RList listArgs(Object... args) {
        return listArgs(getSuppliedSignature(), args);
    }

    @Specialization(guards = "!missing(args)")
    protected RList list(RArgsValuesAndNames args) {
        return listArgs(args.getSignature(), args.getArguments());
    }

    @Specialization
    protected RList list(@SuppressWarnings("unused") RMissing missing) {
        controlVisibility();
        return RDataFactory.createList(new Object[]{});
    }

    @Specialization
    protected RList list(byte value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(int value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(double value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(RRaw value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(RComplex value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(String value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(RAbstractVector value) {
        return listArgs(value);
    }

    @Specialization(guards = "missing(args)")
    protected RList listMissing(@SuppressWarnings("unused") RArgsValuesAndNames args) {
        return list(RMissing.instance);
    }

    @Specialization
    protected RList list(RNull value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(REnvironment value) {
        return listArgs(value);
    }

    @Specialization
    protected RList list(RFunction value) {
        return listArgs(value);
    }

    private RStringVector argNameVector(ArgumentsSignature signature) {
        if (namesNull.profile(signature.getNonNullCount() == 0)) {
            return null;
        }
        String[] names = new String[signature.getLength()];
        for (int i = 0; i < names.length; i++) {
            String orgName = signature.getName(i);
            names[i] = (orgName == null ? RRuntime.NAMES_ATTR_EMPTY_VALUE : orgName);
        }
        return RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR);
    }

    protected boolean missing(RArgsValuesAndNames args) {
        return args.isEmpty();
    }
}
