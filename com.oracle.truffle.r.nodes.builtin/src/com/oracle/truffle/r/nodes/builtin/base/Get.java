/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * assert: not expected to be fast even when called as {@code get("x")}.
 */
@RBuiltin(name = "get", kind = INTERNAL, parameterNames = {"x", "envir", "mode", "inherits"})
public abstract class Get extends RBuiltinNode {

    private final ValueProfile modeProfile = ValueProfile.createIdentityProfile();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile inheritsProfile = BranchProfile.create();

    public abstract Object execute(VirtualFrame frame, RAbstractStringVector name, REnvironment envir, String mode, byte inherits);

    @Specialization(guards = "!isInherits")
    protected Object getNonInherit(RAbstractStringVector xv, REnvironment envir, String mode, @SuppressWarnings("unused") byte inherits) {
        controlVisibility();
        return getAndCheck(xv, envir, mode, true);
    }

    @Specialization(guards = "isInherits")
    protected Object getInherit(RAbstractStringVector xv, REnvironment envir, String mode, @SuppressWarnings("unused") byte inherits) {
        controlVisibility();
        Object r = getAndCheck(xv, envir, mode, false);
        if (r == null) {
            inheritsProfile.enter();
            String x = xv.getDataAt(0);
            RType modeType = RType.fromString(mode);
            REnvironment env = envir;
            while (env != null) {
                env = env.getParent();
                if (env != null) {
                    r = env.get(x);
                    if (r != null && RRuntime.checkType(r, modeType)) {
                        break;
                    }
                }
            }
            if (r == null) {
                unknownObject(x, modeType);
            }
        }
        return r;
    }

    private void unknownObject(String x, RType modeType) throws RError {
        errorProfile.enter();
        if (modeType == RType.Any) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_OBJECT, x);
        } else {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_OBJECT_MODE, x, modeType.getName());
        }
    }

    private Object getAndCheck(RAbstractStringVector xv, REnvironment env, String mode, boolean fail) throws RError {
        String x = xv.getDataAt(0);
        RType modeType = RType.fromString(modeProfile.profile(mode));
        Object obj = env.get(x);
        if (obj != null && RRuntime.checkType(obj, modeType)) {
            return obj;
        } else {
            if (fail) {
                unknownObject(x, modeType);
            }
            return null;
        }
    }

    public static boolean isInherits(@SuppressWarnings("unused") RAbstractStringVector x, @SuppressWarnings("unused") REnvironment envir, String mode, byte inherits) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

}
