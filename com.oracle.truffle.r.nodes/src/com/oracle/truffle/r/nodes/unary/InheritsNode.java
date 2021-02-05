/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.ImplicitClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.IsNotObject;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Basic support for "inherits" that is used by the {@code inherits} builtin and others.
 */
@TypeSystemReference(RTypes.class)
public abstract class InheritsNode extends RBaseNode {

    @Child private ClassHierarchyNode classHierarchy;

    public abstract Object execute(Object x, RStringVector what, boolean which);

    protected static boolean isNotImplicitClass(String className) {
        return !ImplicitClassHierarchyNode.isImplicitClass(className);
    }

    protected static boolean vectorEquals(VectorAccess access, RAbstractVector vec, String what) {
        SequentialIterator it = access.access(vec);
        access.next(it);
        return access.getLength(it) == 1 && access.getString(it).equals(what);
    }

    protected static String getSingleOrNull(RStringVector vec) {
        return vec.getLength() == 1 ? vec.getDataAt(0) : null;
    }

    // Fast path for situation where: "what" is a constant and "x" is not an object
    // We need to check that "what" is not implicit class that's why we cannot just simply check
    // that "x" is not an object. Note IsNotObject covers S4 classes too.
    @Specialization(guards = {
                    "!which",
                    "cachedWhat != null",
                    "isNotObject.execute(x)",
                    "whatAccess.supports(what)",
                    "vectorEquals(whatAccess, what, cachedWhat)",
                    "isNotImplicitClass(cachedWhat)"}, limit = "getVectorAccessCacheSize()")
    @SuppressWarnings("unused") // all arguments are used only in the guard
    protected byte noObjectFastPath(Object x, RStringVector what, boolean which,
                    @Cached IsNotObject isNotObject,
                    @Cached("what.access()") VectorAccess whatAccess,
                    @Cached("getSingleOrNull(what)") String cachedWhat) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(guards = {"!which", "whatAccess.supports(what)"}, limit = "getVectorAccessCacheSize()")
    protected byte doesInherit(Object x, RStringVector what, @SuppressWarnings("unused") boolean which,
                    @Cached BranchProfile nonEmptyClassHierarchy,
                    @Cached("createIdentityProfile()") ValueProfile whatValueProfile,
                    @Cached("createBinaryProfile()") ConditionProfile whatIsSingleElement,
                    @Cached ContainsCheck containsCheck,
                    @Cached("what.access()") VectorAccess whatAccess) {
        RStringVector hierarchy = getClassHierarchy().execute(x);
        if (hierarchy == null) {
            return RRuntime.LOGICAL_FALSE;
        }
        nonEmptyClassHierarchy.enter();
        SequentialIterator whatIt = whatAccess.access(what);
        if (whatIsSingleElement.profile(whatAccess.getLength(whatIt) == 1)) {
            whatAccess.next(whatIt);
            String whatString = whatValueProfile.profile(whatAccess.getString(whatIt));
            return RRuntime.asLogical(containsCheck.execute(whatString, hierarchy));
        }
        while (whatAccess.next(whatIt)) {
            String whatString = whatValueProfile.profile(whatAccess.getString(whatIt));
            if (containsCheck.execute(whatString, hierarchy)) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization(replaces = {"doesInherit", "noObjectFastPath"}, guards = "!which")
    protected byte doesInheritGeneric(Object x, RStringVector what, @SuppressWarnings("unused") boolean which,
                    @Cached ContainsCheck containsCheck) {
        return doesInherit(x, what, which, BranchProfile.getUncached(), ValueProfile.getUncached(), ConditionProfile.getUncached(), containsCheck, what.slowPathAccess());
    }

    @Specialization(guards = "which")
    protected RIntVector doesInheritWhich(Object x, RStringVector what, @SuppressWarnings("unused") boolean which) {
        RStringVector hierarchy = getClassHierarchy().execute(x);
        int[] data = new int[what.getLength()];
        for (int i = 0; i < what.getLength(); i++) {
            String whatString = what.getDataAt(i);
            for (int j = 0; j < hierarchy.getLength(); j++) {
                if (whatString.equals(hierarchy.getDataAt(j))) {
                    data[i] = j + 1;
                    break;
                }
            }
        }
        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    public ClassHierarchyNode getClassHierarchy() {
        if (classHierarchy == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchy = insert(ClassHierarchyNodeGen.create(true, true));
        }
        return classHierarchy;
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class ContainsCheck extends Node {
        public abstract boolean execute(String needle, RStringVector haystack);

        @Specialization(guards = {"haystackAccess.supports(haystack)"}, limit = "getVectorAccessCacheSize()")
        protected boolean doLongHaystack(String needle, RStringVector haystack,
                        @Cached("haystack.access()") VectorAccess haystackAccess) {
            SequentialIterator it = haystackAccess.access(haystack);
            while (haystackAccess.next(it)) {
                if (needle.equals(haystackAccess.getString(it))) {
                    return true;
                }
            }
            return false;
        }

        @Specialization(replaces = "doLongHaystack")
        protected boolean doLongHaystackGeneric(String needle, RStringVector haystack) {
            return doLongHaystack(needle, haystack, haystack.slowPathAccess());
        }
    }
}
