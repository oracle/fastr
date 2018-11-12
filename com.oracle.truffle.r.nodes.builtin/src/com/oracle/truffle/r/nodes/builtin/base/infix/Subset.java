/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.convertIndex;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUBSET;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SubsetSpecial;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SubsetSpecial2;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "[", kind = PRIMITIVE, parameterNames = {"x", "...", "drop"}, dispatch = INTERNAL_GENERIC, behavior = PURE_SUBSET, allowMissingInVarArgs = true)
public abstract class Subset extends RBuiltinNode.Arg3 {

    @RBuiltin(name = ".subset", kind = PRIMITIVE, parameterNames = {"", "...", "drop"}, behavior = PURE_SUBSET)
    public abstract class DefaultBuiltin {
        // same implementation as "[", with different dispatch
    }

    public static RNode special(ArgumentsSignature signature, RNode[] args, boolean inReplacement) {
        if (signature.getNonNullCount() == 0 && (args.length == 2 || args.length == 3)) {
            ConvertIndex index = convertIndex(args[1]);
            if (args.length == 2) {
                return SubsetSpecial.create(inReplacement, args[0], index);
            } else {
                return SubsetSpecial2.create(inReplacement, args[0], index, convertIndex(args[2]));
            }
        }
        return null;
    }

    @Child private ExtractVectorNode extractNode = ExtractVectorNode.create(ElementAccessMode.SUBSET, false);

    static {
        Casts casts = new Casts(Subset.class);
        casts.arg("drop").asLogicalVector();
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull get(RNull x, Object indexes, Object drop) {
        return x;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "indexes.isEmpty()")
    protected Object getNoInd(Object x, RArgsValuesAndNames indexes, Object drop) {
        return x;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object get(Object x, RMissing indexes, Object drop) {
        return x;
    }

    @Specialization(guards = "!indexes.isEmpty()")
    protected Object get(Object x, RArgsValuesAndNames indexes, Object drop) {
        Object[] args = indexes.getArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == RMissing.instance) {
                args[i] = REmpty.instance;
            }
        }
        return extractNode.apply(x, args, RLogical.TRUE, drop);
    }
}
