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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.convertSubset;
import static com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.profile;
import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ConvertIndex;
import com.oracle.truffle.r.nodes.builtin.base.infix.SpecialsUtils.ProfiledValue;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Subset special only handles single element integer/double index. In the case of list, we need to
 * create the actual list otherwise we just return the primitive type.
 */
abstract class SubsetSpecial extends SubscriptSpecialBase {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected SubsetSpecial(boolean inReplacement) {
        super(inReplacement);
    }

    @Override
    protected boolean simpleVector(RAbstractVector vector) {
        return super.simpleVector(vector) && getNamesNode.getNames(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index)", "!inReplacement"})
    protected static RList access(RList vector, int index,
                    @Cached("create()") ExtractListElement extract) {
        return RDataFactory.createList(new Object[]{extract.execute(vector, index - 1)});
    }

    protected static ExtractVectorNode createAccess() {
        return ExtractVectorNode.create(ElementAccessMode.SUBSET, false);
    }

    @Specialization(guards = {"simpleVector(vector)", "!inReplacement"})
    protected static Object access(VirtualFrame frame, RAbstractVector vector, Object index,
                    @Cached("createAccess()") ExtractVectorNode extract) {
        return extract.apply(frame, vector, new Object[]{index}, RRuntime.LOGICAL_TRUE, RLogical.TRUE);
    }
}

/**
 * Subset special only handles single element integer/double index. In the case of list, we need to
 * create the actual list otherwise we just return the primitive type.
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
abstract class SubsetSpecial2 extends SubscriptSpecial2Base {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    protected SubsetSpecial2(boolean inReplacement) {
        super(inReplacement);
    }

    @Override
    protected boolean simpleVector(RAbstractVector vector) {
        return super.simpleVector(vector) && getNamesNode.getNames(vector) == null;
    }

    @Specialization(guards = {"simpleVector(vector)", "isValidIndex(vector, index1, index2)", "!inReplacement"})
    protected RList access(RList vector, int index1, int index2,
                    @Cached("create()") ExtractListElement extract) {
        return RDataFactory.createList(new Object[]{extract.execute(vector, matrixIndex(vector, index1, index2))});
    }
}

@RBuiltin(name = "[", kind = PRIMITIVE, parameterNames = {"x", "...", "drop"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
public abstract class Subset extends RBuiltinNode {

    @RBuiltin(name = ".subset", kind = PRIMITIVE, parameterNames = {"", "...", "drop"}, behavior = PURE)
    public abstract class DefaultBuiltin {
        // same implementation as "[", with different dispatch
    }

    public static RNode special(ArgumentsSignature signature, RNode[] args, boolean inReplacement) {
        if (signature.getNonNullCount() == 0 && (args.length == 2 || args.length == 3)) {
            ProfiledValue profiledVector = profile(args[0]);
            ConvertIndex index = convertSubset(args[1]);
            if (args.length == 2) {
                return SubsetSpecialNodeGen.create(inReplacement, profiledVector, index);
            } else {
                return SubsetSpecial2NodeGen.create(inReplacement, profiledVector, index, convertSubset(args[2]));
            }
        }
        return null;
    }

    @Child private ExtractVectorNode extractNode = ExtractVectorNode.create(ElementAccessMode.SUBSET, false);

    @Override
    protected void createCasts(CastBuilder casts) {
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
    protected Object get(VirtualFrame frame, Object x, RArgsValuesAndNames indexes, Object drop) {
        return extractNode.apply(frame, x, indexes.getArguments(), RLogical.TRUE, drop);
    }
}
