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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = "list", kind = PRIMITIVE, parameterNames = {"..."})
public abstract class ListBuiltin extends RBuiltinNode {

    protected static final int CACHE_LIMIT = 2;
    protected static final int MAX_PROFILES = 16;

    @CompilationFinal private final ValueProfile[] valueProfiles = new ValueProfile[MAX_PROFILES];
    private final ConditionProfile namesNull = ConditionProfile.createBinaryProfile();
    private final BranchProfile shareable = BranchProfile.create();
    private final BranchProfile temporary = BranchProfile.create();

    @CompilationFinal private RStringVector suppliedSignatureArgNames;

    protected RStringVector argNameVector(ArgumentsSignature signature) {
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

    private void shareListElement(Object value) {
        if (value instanceof RShareable) {
            shareable.enter();
            if (((RShareable) value).isTemporary()) {
                temporary.enter();
                ((RShareable) value).incRefCount();
            }
        }
    }

    /**
     * This specialization unrolls the loop that shares the list elements, uses value profiles for
     * each element, and keeps a cached version of the name vector.
     */
    @Specialization(limit = "CACHE_LIMIT", guards = {"!args.isEmpty()", //
                    "args.getLength() <= MAX_PROFILES", //
                    "cachedLength == args.getLength()", //
                    "cachedSignature == args.getSignature()"})
    @ExplodeLoop
    protected RList listCached(RArgsValuesAndNames args, //
                    @Cached("args.getLength()") int cachedLength, //
                    @SuppressWarnings("unused") @Cached("args.getSignature()") ArgumentsSignature cachedSignature, //
                    @Cached("argNameVector(cachedSignature)") RStringVector cachedArgNames) {
        Object[] argArray = args.getArguments();
        controlVisibility();
        for (int i = 0; i < cachedLength; i++) {
            if (valueProfiles[i] == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                valueProfiles[i] = ValueProfile.createClassProfile();
            }
            shareListElement(valueProfiles[i].profile(argArray[i]));
        }
        return RDataFactory.createList(argArray, cachedArgNames);
    }

    @Specialization(guards = "!args.isEmpty()")
    protected RList list(RArgsValuesAndNames args) {
        Object[] argArray = args.getArguments();
        controlVisibility();
        for (int i = 0; i < argArray.length; i++) {
            shareListElement(argArray[i]);
        }
        return RDataFactory.createList(argArray, argNameVector(args.getSignature()));
    }

    @Specialization(guards = "args.isEmpty()")
    protected RList listMissing(@SuppressWarnings("unused") RArgsValuesAndNames args) {
        return listMissing(RMissing.instance);
    }

    @Specialization
    protected RList listMissing(@SuppressWarnings("unused") RMissing missing) {
        controlVisibility();
        return RDataFactory.createList(new Object[]{});
    }

    @Specialization(guards = {"!isRArgsValuesAndNames(value)", "!isRMissing(value)"})
    protected RList listSingleElement(Object value) {
        controlVisibility();
        shareListElement(value);
        if (suppliedSignatureArgNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            suppliedSignatureArgNames = argNameVector(ArgumentsSignature.empty(1));
        }
        return RDataFactory.createList(new Object[]{value}, suppliedSignatureArgNames);
    }
}
