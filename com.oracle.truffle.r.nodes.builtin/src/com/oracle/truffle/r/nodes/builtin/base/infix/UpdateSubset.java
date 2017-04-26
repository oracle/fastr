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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.convertIndex;
import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.convertValue;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ReplaceVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "[<-", kind = PRIMITIVE, parameterNames = {"..."}, dispatch = INTERNAL_GENERIC, behavior = PURE)
public abstract class UpdateSubset extends RBuiltinNode.Arg1 {

    @Child private ReplaceVectorNode replaceNode = ReplaceVectorNode.create(ElementAccessMode.SUBSET, false);
    private final ConditionProfile argsLengthLargerThanOneProfile = ConditionProfile.createBinaryProfile();

    static {
        Casts.noCasts(UpdateSubset.class);
    }

    public static RNode special(ArgumentsSignature signature, RNode[] args, boolean inReplacement) {
        if (SpecialsUtils.isCorrectUpdateSignature(signature) && (args.length == 3 || args.length == 4)) {
            ConvertIndex index = convertIndex(args[1]);
            if (args.length == 3) {
                return UpdateSubscriptSpecial.create(inReplacement, args[0], index, convertValue(args[2]));
            } else {
                return UpdateSubscriptSpecial2.create(inReplacement, args[0], index, convertIndex(args[2]), convertValue(args[3]));
            }
        }
        return null;
    }

    @Specialization(guards = "args.getLength() >= 2")
    protected Object update(VirtualFrame frame, RArgsValuesAndNames args) {
        // first argument: object to assign to
        Object x = args.getArgument(0);

        // last argument: value to assign
        Object value = args.getArgument(args.getLength() - 1);

        // arguments between first and last are assumed to be indices
        Object[] pos;
        if (argsLengthLargerThanOneProfile.profile(args.getLength() > 2)) {
            pos = Arrays.copyOfRange(args.getArguments(), 1, args.getLength() - 1);
        } else {
            pos = new Object[]{RMissing.instance};
        }
        return replaceNode.apply(frame, x, pos, value);
    }

    @Specialization(guards = "args.getLength() < 2")
    @SuppressWarnings("unused")
    protected Object getNoInd(RArgsValuesAndNames args) {
        throw error(RError.Message.INVALID_ARG_NUMBER, "SubAssignArgs");
    }
}
