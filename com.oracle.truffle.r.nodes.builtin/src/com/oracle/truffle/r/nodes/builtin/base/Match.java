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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalFalse;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.runtime.RError.Message.MATCH_VECTOR_ARGS;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.MatchInternalNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.helpers.RFactorNodes;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/*
 * TODO: handle "incomparables" parameter.
 */
@RBuiltin(name = "match", kind = INTERNAL, parameterNames = {"x", "table", "nomatch", "incomparables"}, behavior = PURE)
public abstract class Match extends RBuiltinNode.Arg4 {

    protected abstract Object executeRIntVector(Object x, Object table, Object noMatch, Object incomparables);

    static {
        Casts casts = new Casts(Match.class);
        // TODO initially commented out because of use of scalars, the commented out version
        // converted to new cast pipelines API

        // casts.arg("x").allowNull().mustBe(abstractVectorValue(), SHOW_CALLER,
        // MATCH_VECTOR_ARGS).asVectorPreserveAttrs(true);
        // casts.arg("table").allowNull().mustBe(abstractVectorValue()).asVectorPreserveAttrs(true);
        casts.arg("nomatch").asIntegerVector().findFirst();
        casts.arg("incomparables").defaultError(Message.GENERIC, "usage of 'incomparables' in match not implemented").allowNull().mustBe(logicalValue()).asLogicalVector().findFirst().mustBe(
                        logicalFalse());
    }

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
    protected RIntVector match(Object x, Object table, Object nomatch, Object incomparables) {
        throw error(MATCH_VECTOR_ARGS);
    }
}
