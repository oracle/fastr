/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_FRAME;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

@RBuiltin(name = "...elt", kind = PRIMITIVE, parameterNames = {"n"}, behavior = READS_FRAME)
public abstract class DotDotDotElt extends RBuiltinNode.Arg1 {

    @Child private ReadVariableNode lookupVarArgs;
    @Child private PromiseHelperNode promiseHelper;
    private final ConditionProfile isRPromise = ConditionProfile.createBinaryProfile();

    static {
        Casts casts = new Casts(DotDotDotElt.class);
        casts.arg("n").asIntegerVector().findFirst();
    }

    @Specialization(guards = "n > 0")
    protected Object lookupElt(VirtualFrame frame, int n) {
        if (lookupVarArgs == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookupVarArgs = ReadVariableNode.createSilent(ArgumentsSignature.VARARG_NAME, RType.Any);
        }
        Object value = lookupVarArgs.execute(frame);
        if (value == RNull.instance || value == null) {
            noElementsError();
        }
        RArgsValuesAndNames varArgs = (RArgsValuesAndNames) value;
        if (varArgs.getLength() == 0) {
            noElementsError();
        }
        int zeroBased = n - 1;
        if (zeroBased > varArgs.getLength()) {
            throw RError.error(RError.SHOW_CALLER, Message.DOT_DOT_SHORT, n);
        }

        Object arg = varArgs.getArgument(zeroBased);

        if (isRPromise.profile(arg instanceof RPromise)) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            arg = promiseHelper.visibleEvaluate(frame, (RPromise) arg);
        }

        return arg;
    }

    @Specialization(guards = "n <= 0")
    protected Object lookupEltErr(int n) {
        throw RError.error(RError.SHOW_CALLER, Message.DOT_DOT_INDEX_ZERO_OR_LESS, n);
    }

    private static void noElementsError() {
        throw RError.error(RError.SHOW_CALLER, Message.DOT_DOT_NONE);
    }

    public static DotDotDotElt create() {
        return DotDotDotEltNodeGen.create();
    }
}
