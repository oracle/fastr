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

import com.oracle.truffle.api.dsl.Cached;
import static com.oracle.truffle.r.nodes.builtin.base.infix.special.SpecialsUtils.convertIndex;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_SUBSCRIPT;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SubscriptSpecial;
import com.oracle.truffle.r.nodes.builtin.base.infix.special.SubscriptSpecial2;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "[[", kind = PRIMITIVE, parameterNames = {"x", "...", "exact", "drop"}, dispatch = INTERNAL_GENERIC, behavior = PURE_SUBSCRIPT, allowMissingInVarArgs = true)
@TypeSystemReference(RTypes.class)
public abstract class Subscript extends RBuiltinNode.Arg4 {

    protected static final int CACHE_LIMIT = DSLConfig.getCacheSize(3);

    @RBuiltin(name = ".subset2", kind = PRIMITIVE, parameterNames = {"x", "...", "exact", "drop"}, behavior = PURE_SUBSCRIPT)
    public abstract class DefaultBuiltin {
        // same implementation as "[[", with different dispatch
    }

    static {
        Casts.noCasts(Subscript.class);
    }

    public static RNode special(ArgumentsSignature signature, RNode[] arguments, boolean inReplacement) {
        if (signature.getNonNullCount() == 0) {
            if (arguments.length == 2) {
                return SubscriptSpecial.create(inReplacement, arguments[0], convertIndex(arguments[1]));
            } else if (arguments.length == 3) {
                return SubscriptSpecial2.create(inReplacement, arguments[0], convertIndex(arguments[1]), convertIndex(arguments[2]));
            }
        }
        return null;
    }

    @Child private ExtractVectorNode extractNode = ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RArgsValuesAndNames.EMPTY, RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_TRUE};
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull get(RNull x, Object inds, Object exactVec, Object drop) {
        return x;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "indexes.isEmpty()")
    protected Object getNoInd(Object x, RArgsValuesAndNames indexes, RAbstractLogicalVector exact, RAbstractLogicalVector drop) {
        throw error(RError.Message.NO_INDEX);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object get(Object x, RMissing indexes, RAbstractLogicalVector exact, RAbstractLogicalVector drop) {
        throw error(RError.Message.NO_INDEX);
    }

    @Specialization(guards = {"!indexes.isEmpty()", "argsLen == indexes.getLength()"}, limit = "CACHE_LIMIT")
    @ExplodeLoop
    protected Object getIndexes(Object x, RArgsValuesAndNames indexes, RAbstractLogicalVector exact, @SuppressWarnings("unused") Object drop,
                    @Cached("indexes.getLength()") int argsLen) {
        /*
         * "drop" is not actually used by this builtin, but it needs to be in the argument list
         * (because the "drop" argument needs to be skipped).
         */
        Object[] args = indexes.getArguments();
        for (int i = 0; i < argsLen; i++) {
            if (args[i] == RMissing.instance) {
                args[i] = REmpty.instance;
            }
        }
        return extractNode.apply(x, args, exact, RLogical.TRUE);
    }

    @Specialization(guards = "!indexes.isEmpty()", replaces = "getIndexes")
    protected Object getIndexesGeneric(Object x, RArgsValuesAndNames indexes, RAbstractLogicalVector exact, @SuppressWarnings("unused") Object drop) {
        /*
         * "drop" is not actually used by this builtin, but it needs to be in the argument list
         * (because the "drop" argument needs to be skipped).
         */
        Object[] args = indexes.getArguments();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == RMissing.instance) {
                args[i] = REmpty.instance;
            }
        }
        return extractNode.apply(x, args, exact, RLogical.TRUE);
    }
}
