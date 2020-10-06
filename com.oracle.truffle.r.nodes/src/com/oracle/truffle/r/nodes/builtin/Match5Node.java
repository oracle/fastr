/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import static com.oracle.truffle.r.runtime.RError.Message.MATCH_VECTOR_ARGS;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Implements the {@code match5} function internal to R, but nonetheless referenced by some R
 * packages. This function should take 5 arguments where the 5th argument is an environment, which
 * allows to run as.character on {@code POSIXlt} objects to transform the {@code x} and
 * {@code table} arguments. Another transformation of {@code x} and {@code table} arguments is from
 * factors to string vectors, which we implement here, but for which we do not need the environment.
 */
@TypeSystemReference(RTypes.class)
public abstract class Match5Node extends RBaseNode {

    public abstract Object execute(Object x, Object table, int noMatch, Object incomparables);

    protected boolean isCharSXP(RAbstractListVector list) {
        for (int i = 0; i < list.getLength(); i++) {
            if (!(RType.getRType(list.getDataAt(i)).equals(RType.Char))) {
                return false;
            }
        }
        return true;
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RNull x, RNull table, int nomatch, Object incomparables) {
        return RDataFactory.createIntVector(0);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RNull x, RAbstractVector table, int nomatch, Object incomparables) {
        return RDataFactory.createIntVector(0);
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RIntVector match(RAbstractVector x, RNull table, int nomatch, Object incomparables,
                    @Cached("createBinaryProfile()") ConditionProfile na) {
        int[] data = new int[x.getLength()];
        Arrays.fill(data, nomatch);
        return RDataFactory.createIntVector(data, na.profile(!RRuntime.isNA(nomatch)));
    }

    @Child private InheritsCheckNode factorInheritsCheck = InheritsCheckNode.create(RRuntime.CLASS_FACTOR);

    protected boolean isFactor(Object o) {
        return factorInheritsCheck.execute(o);
    }

    @Specialization(guards = {"isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RIntVector x, RIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached() MatchInternalNode match) {
        return match.execute(RClosures.createFactorToVector(x, true, getLevelsNode.execute(x)),
                        RClosures.createFactorToVector(table, true, getLevelsNode.execute(table)), nomatch);
    }

    @Specialization(guards = {"isFactor(x)", "!isFactor(table)"})
    protected Object matchFactor(RIntVector x, RAbstractVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached() MatchInternalNode match) {
        return match.execute(RClosures.createFactorToVector(x, true, getLevelsNode.execute(x)), table, nomatch);
    }

    @Specialization(guards = {"!isFactor(x)", "isFactor(table)"})
    protected Object matchFactor(RAbstractVector x, RIntVector table, int nomatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") RFactorNodes.GetLevels getLevelsNode,
                    @Cached() MatchInternalNode match) {
        return match.execute(x, RClosures.createFactorToVector(table, true, getLevelsNode.execute(table)), nomatch);
    }

    @Specialization(guards = {"isCharSXP(x)", "isCharSXP(table)"})
    protected Object matchDoubleList(RAbstractListVector x, RAbstractListVector table, int nomatchObj, @SuppressWarnings("unused") Object incomparables,
                    @Cached() MatchInternalNode match) {
        return match.execute(x, table, nomatchObj);
    }

    @Specialization(guards = {"!isRIntVector(table) || !isFactor(table)"})
    protected Object matchList(RAbstractListVector x, RAbstractVector table, int nomatchObj, @SuppressWarnings("unused") Object incomparables,
                    @Cached("create()") CastStringNode cast,
                    @Cached() MatchInternalNode match) {
        return match.execute((RAbstractVector) cast.doCast(x), table, nomatchObj);
    }

    @Specialization(guards = {"!isRAbstractListVector(x)", "!isRIntVector(x) || !isFactor(x)", "!isRIntVector(table) || !isFactor(table)"})
    protected Object match(RAbstractVector x, RAbstractVector table, int noMatch, @SuppressWarnings("unused") Object incomparables,
                    @Cached() MatchInternalNode match) {
        return match.execute(x, table, noMatch);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected RIntVector match(Object x, Object table, int nomatch, Object incomparables) {
        throw error(MATCH_VECTOR_ARGS);
    }
}
