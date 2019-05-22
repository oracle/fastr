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
package com.oracle.truffle.r.runtime.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.GetForeignMembersNodeGen.ReadMemberElementsNodeGen;
import com.oracle.truffle.r.runtime.interop.GetForeignMembersNodeGen.ReadMembersNodeGen;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic({RRuntime.class})
public abstract class GetForeignMembersNode extends RBaseNode {

    public static GetForeignMembersNode create() {
        return GetForeignMembersNodeGen.create();
    }

    public abstract Object execute(Object obj, boolean acceptJavaStatic);

    @Specialization(guards = {"isForeignObject(obj)", "interop.hasMembers(obj)", "acceptJavaStatic"}, limit = "getInteropLibraryCacheSize()")
    protected Object getMembers(TruffleObject obj, @SuppressWarnings("unused") boolean acceptJavaStatic,
                    @Cached("create()") ToJavaStaticNode toJavaStatic,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop,
                    @Cached("create()") ReadMembersNode readStaticMembers,
                    @Cached("create()") ReadMembersNode readMembers) {

        TruffleObject clazzStatic = toJavaStatic.execute(obj);
        String[] staticNames = clazzStatic != null ? readStaticMembers.execute(clazzStatic) : new String[0];
        String[] names = readMembers.execute(obj);
        if (names.length == 0 && staticNames.length == 0) {
            return RNull.instance;
        }
        String[] result = new String[names.length + staticNames.length];
        System.arraycopy(names, 0, result, 0, names.length);
        System.arraycopy(staticNames, 0, result, names.length, staticNames.length);
        return RDataFactory.createStringVector(result, true);
    }

    @Specialization(guards = {"isForeignObject(obj)", "interop.hasMembers(obj)", "!acceptJavaStatic"}, limit = "getInteropLibraryCacheSize()")
    protected Object getMembers(TruffleObject obj, @SuppressWarnings("unused") boolean acceptJavaStatic,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop,
                    @Cached("create()") ReadMembersNode readMembers) {
        String[] names = readMembers.execute(obj);
        if (names.length == 0) {
            return RNull.instance;
        }
        return RDataFactory.createStringVector(names, true);
    }

    @Specialization(guards = {"isForeignObject(obj)", "!interop.hasMembers(obj)"}, limit = "getInteropLibraryCacheSize()")
    protected Object noMembers(@SuppressWarnings("unused") TruffleObject obj, @SuppressWarnings("unused") boolean acceptJavaStatic,
                    @SuppressWarnings("unused") @CachedLibrary("obj") InteropLibrary interop) {
        return RNull.instance;
    }

    @Fallback
    public Object doObject(@SuppressWarnings("unused") Object obj, @SuppressWarnings("unused") boolean acceptJavaStatic) {
        return RInternalError.shouldNotReachHere();
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class ReadMembersNode extends Node {
        protected static ReadMembersNode create() {
            return ReadMembersNodeGen.create();
        }

        public abstract String[] execute(Object obj);

        @Specialization(limit = "getInteropLibraryCacheSize()")
        String[] readMembers(Object obj,
                        @CachedLibrary("obj") InteropLibrary interop,
                        @Cached("create()") ReadMemberElementsNode readElements) {
            try {
                return readElements.execute(interop.getMembers(obj));
            } catch (UnsupportedMessageException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

    @ImportStatic(DSLConfig.class)
    protected abstract static class ReadMemberElementsNode extends Node {
        protected static ReadMemberElementsNode create() {
            return ReadMemberElementsNodeGen.create();
        }

        public abstract String[] execute(Object obj);

        @Specialization(limit = "getInteropLibraryCacheSize()")
        String[] readMembers(Object members,
                        @CachedLibrary("members") InteropLibrary interop) {
            try {
                int size = RRuntime.getForeignArraySize(members, interop);
                String[] names = new String[size];
                for (int i = 0; i < size; i++) {
                    Object value = interop.readArrayElement(members, i);
                    names[i] = (String) value;
                }
                return names;
            } catch (InteropException ex) {
                throw RInternalError.shouldNotReachHere();
            }
        }
    }

}
